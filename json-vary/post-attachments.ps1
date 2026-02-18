<#
.SYNOPSIS
    POST all attachment files in a single multipart request.

.DESCRIPTION
    Collects all non-CSV files from the attachments directory and sends them
    in one multipart/form-data POST request, each as a 'files' field.

.PARAMETER Uri
    The endpoint URL to POST to.

.PARAMETER AuthToken
    The full Basic auth string (e.g., 'Basic On53YF0/fGA4SS9JQCUlIX51MEUq').

.PARAMETER AttachmentsDir
    Directory containing attachment files to upload.

.PARAMETER SkipCertCheck
    Skip SSL certificate validation (for localhost / self-signed certs).

.EXAMPLE
    .\post-attachments.ps1 -Uri 'https://localhost:8080/api/cavr/attachments/s3files' `
        -AuthToken 'Basic On53YF0/fGA4SS9JQCUlIX51MEUq' `
        -AttachmentsDir './output/attachments'
#>

[CmdletBinding()]
param(
    [Parameter(Mandatory=$true)]
    [string]$Uri,

    [Parameter(Mandatory=$true)]
    [string]$AuthToken,

    [Parameter(Mandatory=$true)]
    [string]$AttachmentsDir,

    [switch]$SkipCertCheck
)

$ErrorActionPreference = "Stop"

# Skip SSL cert validation if requested
if ($SkipCertCheck) {
    Add-Type @"
using System.Net;
using System.Security.Cryptography.X509Certificates;
public class TrustAll : ICertificatePolicy {
    public bool CheckValidationResult(ServicePoint sp, X509Certificate cert,
        WebRequest req, int problem) { return true; }
}
"@
    [System.Net.ServicePointManager]::CertificatePolicy = New-Object TrustAll
}

# Content-type mapping
$contentTypes = @{
    ".pdf"  = "application/pdf"
    ".json" = "application/json"
    ".jpeg" = "image/jpeg"
    ".jpg"  = "image/jpeg"
    ".png"  = "image/png"
    ".xlsx" = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    ".xls"  = "application/vnd.ms-excel"
    ".docx" = "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    ".doc"  = "application/msword"
    ".pptx" = "application/vnd.openxmlformats-officedocument.presentationml.presentation"
    ".ods"  = "application/vnd.oasis.opendocument.spreadsheet"
    ".odt"  = "application/vnd.oasis.opendocument.text"
    ".odp"  = "application/vnd.oasis.opendocument.presentation"
}

# Parse URI for headers
$parsedUri = [System.Uri]$Uri
$authority = $parsedUri.Authority
$scheme = $parsedUri.Scheme
$pathAndQuery = $parsedUri.PathAndQuery

# Validate directory
if (-not (Test-Path $AttachmentsDir)) {
    Write-Error "Attachments directory not found: $AttachmentsDir"
    exit 1
}

# Collect non-CSV files
$files = Get-ChildItem -Path $AttachmentsDir -File | Where-Object { $_.Extension -ne ".csv" } | Sort-Object Name

if ($files.Count -eq 0) {
    Write-Error "No attachment files found in: $AttachmentsDir"
    exit 1
}

$totalSize = ($files | Measure-Object -Property Length -Sum).Sum
$totalSizeMB = [math]::Round($totalSize / 1MB, 2)

Write-Host "Endpoint:    $Uri"
Write-Host "Attachments: $AttachmentsDir"
Write-Host "Files:       $($files.Count) ($totalSizeMB MB)"
Write-Host ""

# Build multipart body with all files
$boundary = "----WebKitFormBoundary" + [guid]::NewGuid().ToString("N").Substring(0, 16)
$crlf = "$([char]13)$([char]10)"

# Use a MemoryStream to handle binary file content
$ms = New-Object System.IO.MemoryStream
$encoding = [System.Text.Encoding]::UTF8

foreach ($file in $files) {
    $filename = $file.Name
    $ext = $file.Extension.ToLower()
    $ct = if ($contentTypes.ContainsKey($ext)) { $contentTypes[$ext] } else { "application/octet-stream" }

    # Write part header
    $partHeader = "--${boundary}${crlf}" +
        "Content-Disposition: form-data; name=`"files`"; filename=`"${filename}`"${crlf}" +
        "Content-Type: ${ct}${crlf}" +
        "${crlf}"

    $headerBytes = $encoding.GetBytes($partHeader)
    $ms.Write($headerBytes, 0, $headerBytes.Length)

    # Write file content as raw bytes
    $fileBytes = [System.IO.File]::ReadAllBytes($file.FullName)
    $ms.Write($fileBytes, 0, $fileBytes.Length)

    # Write CRLF after file content
    $crlfBytes = $encoding.GetBytes($crlf)
    $ms.Write($crlfBytes, 0, $crlfBytes.Length)

    Write-Host "  Added: $filename ($ct, $([math]::Round($file.Length / 1KB, 1)) KB)"
}

# Write closing boundary
$closingBytes = $encoding.GetBytes("--${boundary}--${crlf}")
$ms.Write($closingBytes, 0, $closingBytes.Length)

$bodyBytes = $ms.ToArray()
$ms.Dispose()

Write-Host ""
Write-Host "Total body size: $([math]::Round($bodyBytes.Length / 1MB, 2)) MB"
Write-Host "Sending POST..."

$session = New-Object Microsoft.PowerShell.Commands.WebRequestSession
$session.UserAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36 Edg/131.0.0.0"

$headers = @{
    "authority"       = $authority
    "method"          = "POST"
    "path"            = $pathAndQuery
    "scheme"          = $scheme
    "accept"          = "application/hal+json"
    "accept-encoding" = "gzip, deflate, br, zstd"
    "accept-language" = "en-GB,en;q=0.9,en-US;q=0.8"
    "authorization"   = $AuthToken
    "origin"          = "${scheme}://${authority}"
    "priority"        = "u=1, i"
    "referer"         = "${scheme}://${authority}/swagger-ui/index.html"
    "sec-ch-ua"       = "`"Microsoft Edge`";v=`"131`", `"Chromium`";v=`"131`", `"Not_A Brand`";v=`"24`""
    "sec-ch-ua-mobile"   = "?0"
    "sec-ch-ua-platform" = "`"Windows`""
    "sec-fetch-dest"  = "empty"
    "sec-fetch-mode"  = "cors"
    "sec-fetch-site"  = "same-origin"
}

try {
    $response = Invoke-WebRequest -UseBasicParsing -Uri $Uri `
        -Method "POST" `
        -WebSession $session `
        -Headers $headers `
        -ContentType "multipart/form-data; boundary=${boundary}" `
        -Body $bodyBytes

    $statusCode = $response.StatusCode
    $responseSnippet = $response.Content
    if ($responseSnippet.Length -gt 500) {
        $responseSnippet = $responseSnippet.Substring(0, 500) + "..."
    }

    Write-Host ""
    Write-Host "Response: $statusCode OK" -ForegroundColor Green
    Write-Host $responseSnippet
}
catch {
    $statusCode = "ERROR"
    if ($_.Exception.Response) {
        $statusCode = [int]$_.Exception.Response.StatusCode
    }
    Write-Host ""
    Write-Host "Response: $statusCode FAILED" -ForegroundColor Red
    Write-Host $_.Exception.Message
    exit 1
}

Write-Host ""
Write-Host "Done. $($files.Count) files uploaded."
