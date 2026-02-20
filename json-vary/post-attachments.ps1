<#
.SYNOPSIS
    POST attachment files in batched multipart requests.

.DESCRIPTION
    Collects all files from the attachments directory and sends them in
    batched multipart/form-data POST requests. Manifest CSV files are always
    included in the final batch so that they arrive after the files they
    reference.

.PARAMETER Uri
    The endpoint URL to POST to.

.PARAMETER AuthToken
    The full Basic auth string (e.g., 'Basic On53YF0/fGA4SS9JQCUlIX51MEUq').

.PARAMETER AttachmentsDir
    Directory containing attachment files to upload.

.PARAMETER BatchSize
    Maximum number of files per POST request (default 40).

.PARAMETER SkipCertCheck
    Skip SSL certificate validation (for localhost / self-signed certs).

.EXAMPLE
    .\post-attachments.ps1 -Uri 'https://localhost:8080/api/cavr/attachments/s3files' `
        -AuthToken 'Basic On53YF0/fGA4SS9JQCUlIX51MEUq' `
        -AttachmentsDir './output/attachments'

.EXAMPLE
    .\post-attachments.ps1 -Uri 'https://localhost:8080/api/cavr/attachments/s3files' `
        -AuthToken 'Basic On53YF0/fGA4SS9JQCUlIX51MEUq' `
        -AttachmentsDir './output/attachments' `
        -BatchSize 20
#>

[CmdletBinding()]
param(
    [Parameter(Mandatory=$true)]
    [string]$Uri,

    [Parameter(Mandatory=$true)]
    [string]$AuthToken,

    [Parameter(Mandatory=$true)]
    [string]$AttachmentsDir,

    [int]$BatchSize = 40,

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
    ".csv"  = "text/csv"
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

# Validate batch size
if ($BatchSize -lt 1) {
    Write-Error "BatchSize must be a positive integer, got: $BatchSize"
    exit 1
}

# Collect files, separating manifests from attachments
$allFiles = Get-ChildItem -Path $AttachmentsDir -File | Sort-Object Name
$manifestFiles = @($allFiles | Where-Object { $_.Name -match '^manifest.*\.csv$' })
$attachmentFiles = @($allFiles | Where-Object { $_.Name -notmatch '^manifest.*\.csv$' })

if ($allFiles.Count -eq 0) {
    Write-Error "No attachment files found in: $AttachmentsDir"
    exit 1
}

$totalSize = ($allFiles | Measure-Object -Property Length -Sum).Sum
$totalSizeMB = [math]::Round($totalSize / 1MB, 2)

Write-Host "Endpoint:    $Uri"
Write-Host "Attachments: $AttachmentsDir"
Write-Host "Files:       $($allFiles.Count) ($totalSizeMB MB)"
Write-Host "  Attachments: $($attachmentFiles.Count)"
Write-Host "  Manifests:   $($manifestFiles.Count) (sent in final batch)"
Write-Host "Batch size:  $BatchSize"
Write-Host ""

# Build batches: attachment files in groups of BatchSize, manifests appended to the last batch
$batches = @()
for ($i = 0; $i -lt $attachmentFiles.Count; $i += $BatchSize) {
    $end = [math]::Min($i + $BatchSize, $attachmentFiles.Count)
    $batch = @($attachmentFiles[$i..($end - 1)])
    $batches += ,@($batch)
}

# Append manifests to the final batch (or create a manifest-only batch)
if ($batches.Count -eq 0) {
    $batches += ,@($manifestFiles)
} else {
    $batches[$batches.Count - 1] = @($batches[$batches.Count - 1]) + @($manifestFiles)
}

$totalBatches = $batches.Count
Write-Host "Batches:     $totalBatches"
Write-Host ""

# Set up session and headers (reused across batches)
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

$crlf = "$([char]13)$([char]10)"
$encoding = [System.Text.Encoding]::UTF8
$uploadedCount = 0

for ($b = 0; $b -lt $totalBatches; $b++) {
    $batchFiles = $batches[$b]
    $batchNum = $b + 1

    Write-Host "--- Batch $batchNum / $totalBatches ($($batchFiles.Count) files) ---"

    # Build multipart body for this batch
    $boundary = "----WebKitFormBoundary" + [guid]::NewGuid().ToString("N").Substring(0, 16)
    $ms = New-Object System.IO.MemoryStream

    foreach ($file in $batchFiles) {
        $filename = $file.Name
        $ext = $file.Extension.ToLower()
        $ct = if ($contentTypes.ContainsKey($ext)) { $contentTypes[$ext] } else { "application/octet-stream" }

        $partHeader = "--${boundary}${crlf}" +
            "Content-Disposition: form-data; name=`"files`"; filename=`"${filename}`"${crlf}" +
            "Content-Type: ${ct}${crlf}" +
            "${crlf}"

        $headerBytes = $encoding.GetBytes($partHeader)
        $ms.Write($headerBytes, 0, $headerBytes.Length)

        $fileBytes = [System.IO.File]::ReadAllBytes($file.FullName)
        $ms.Write($fileBytes, 0, $fileBytes.Length)

        $crlfBytes = $encoding.GetBytes($crlf)
        $ms.Write($crlfBytes, 0, $crlfBytes.Length)

        Write-Host "  Added: $filename ($ct, $([math]::Round($file.Length / 1KB, 1)) KB)"
    }

    $closingBytes = $encoding.GetBytes("--${boundary}--${crlf}")
    $ms.Write($closingBytes, 0, $closingBytes.Length)

    $bodyBytes = $ms.ToArray()
    $ms.Dispose()

    Write-Host "  Body size: $([math]::Round($bodyBytes.Length / 1MB, 2)) MB"
    Write-Host "  Sending POST..."

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

        Write-Host "  Response: $statusCode OK" -ForegroundColor Green
        Write-Host "  $responseSnippet"
        Write-Host ""
    }
    catch {
        $statusCode = "ERROR"
        if ($_.Exception.Response) {
            $statusCode = [int]$_.Exception.Response.StatusCode
        }
        Write-Host "  Response: $statusCode FAILED" -ForegroundColor Red
        Write-Host "  $($_.Exception.Message)"
        Write-Host ""
        Write-Host "Aborted after $uploadedCount / $($allFiles.Count) files ($batchNum / $totalBatches batches)."
        exit 1
    }

    $uploadedCount += $batchFiles.Count
}

Write-Host "Done. $uploadedCount files uploaded in $totalBatches batch(es)."
