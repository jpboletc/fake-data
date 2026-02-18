<#
.SYNOPSIS
    POST gForm JSON files one-by-one to an endpoint.

.DESCRIPTION
    Iterates all *.json files under the given directory (recursively) and
    POSTs each as a multipart/form-data upload matching the browser-captured
    request format.

.PARAMETER Uri
    The endpoint URL to POST to.

.PARAMETER AuthToken
    The full Basic auth string (e.g., 'Basic On53YF0/fGA4SS9JQCUlIX51MEUq').

.PARAMETER JsonDir
    Root directory containing JSON files (searched recursively).

.PARAMETER SkipCertCheck
    Skip SSL certificate validation (for localhost / self-signed certs).

.EXAMPLE
    .\post-gforms.ps1 -Uri 'https://localhost:8080/api/source-files/upload-in-file' `
        -AuthToken 'Basic On53YF0/fGA4SS9JQCUlIX51MEUq' `
        -JsonDir './output'
#>

[CmdletBinding()]
param(
    [Parameter(Mandatory=$true)]
    [string]$Uri,

    [Parameter(Mandatory=$true)]
    [string]$AuthToken,

    [Parameter(Mandatory=$true)]
    [string]$JsonDir,

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

# Parse URI for headers
$parsedUri = [System.Uri]$Uri
$authority = $parsedUri.Authority
$scheme = $parsedUri.Scheme
$pathAndQuery = $parsedUri.PathAndQuery

# Validate directory
if (-not (Test-Path $JsonDir)) {
    Write-Error "JSON directory not found: $JsonDir"
    exit 1
}

# Find all JSON files recursively
$jsonFiles = Get-ChildItem -Path $JsonDir -Filter "*.json" -Recurse | Sort-Object FullName

if ($jsonFiles.Count -eq 0) {
    Write-Error "No JSON files found under: $JsonDir"
    exit 1
}

Write-Host "Endpoint:   $Uri"
Write-Host "JSON dir:   $JsonDir"
Write-Host "Files found: $($jsonFiles.Count)"
Write-Host ""

$session = New-Object Microsoft.PowerShell.Commands.WebRequestSession
$session.UserAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36 Edg/131.0.0.0"

$successes = 0
$failures = 0
$fileNum = 0

foreach ($jsonFile in $jsonFiles) {
    $fileNum++
    $filename = $jsonFile.Name
    $fileContent = Get-Content -Path $jsonFile.FullName -Raw -Encoding UTF8

    # Build multipart body
    $boundary = "----WebKitFormBoundary" + [guid]::NewGuid().ToString("N").Substring(0, 16)
    $crlf = "$([char]13)$([char]10)"

    $bodyText = "--${boundary}${crlf}" +
        "Content-Disposition: form-data; name=`"file`"; filename=`"${filename}`"${crlf}" +
        "Content-Type: application/json${crlf}" +
        "${crlf}" +
        "${fileContent}${crlf}" +
        "--${boundary}--${crlf}"

    $bodyBytes = [System.Text.Encoding]::UTF8.GetBytes($bodyText)

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
        if ($responseSnippet.Length -gt 200) {
            $responseSnippet = $responseSnippet.Substring(0, 200) + "..."
        }

        Write-Host "  [$fileNum/$($jsonFiles.Count)] $filename -> $statusCode OK"
        $successes++
    }
    catch {
        $statusCode = "ERROR"
        if ($_.Exception.Response) {
            $statusCode = [int]$_.Exception.Response.StatusCode
        }
        Write-Host "  [$fileNum/$($jsonFiles.Count)] $filename -> $statusCode FAILED: $($_.Exception.Message)" -ForegroundColor Red
        $failures++
    }
}

Write-Host ""
Write-Host "Done. $successes succeeded, $failures failed out of $($jsonFiles.Count) files."
