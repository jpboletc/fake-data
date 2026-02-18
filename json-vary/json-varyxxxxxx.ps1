<#
.SYNOPSIS
    Generate N copies of a JSON file with randomised replacements.

.DESCRIPTION
    Each value is a literal string found in the source JSON. The script
    auto-detects its type from the value's format and generates a random
    replacement of the same shape. All occurrences are replaced (string
    matching, not JSON-path matching).

    Pattern detection:
      - All digits (4+ chars)           -> random digits, same length
      - XXXX-XXXX-XXXX (alphanumeric)   -> random alphanumeric groups (submission ref)
      - UUID (8-4-4-4-12 hex)           -> random UUID
      - Anything else                   -> random business name from built-in list

    Output files are named after the generated submission-reference value.
    Produces a summary CSV (vary-manifest.csv) mapping filenames to values.

.PARAMETER SourceFile
    Path to the source JSON file.

.PARAMETER Count
    Number of output copies to generate.

.PARAMETER Values
    One or more literal strings to find and replace in the source JSON.

.PARAMETER Output
    Output directory (default: ./json-vary-output).

.EXAMPLE
    .\json-vary.ps1 creative-relief.json 10 '9237766545' 'T9Q0-IIIB-PP52' 'a8d91e74-2285-4582-9d7c-fe6b400da347' 'SUA tec04'

.EXAMPLE
    .\json-vary.ps1 -Output C:\temp\varied creative-relief.json 5 '9237766545'
#>

param(
    [Parameter(Position=0, Mandatory=$true)]
    [string]$SourceFile,

    [Parameter(Position=1, Mandatory=$true)]
    [int]$Count,

    [Parameter(Position=2, Mandatory=$true, ValueFromRemainingArguments=$true)]
    [string[]]$Values,

    [Alias("o")]
    [string]$Output = "./json-vary-output"
)

$ErrorActionPreference = "Stop"

# --- Built-in business name list ---
$BusinessNames = @(
    "Apex Holdings"
    "Nova Digital"
    "Vertex Group"
    "Pinnacle Corp"
    "Summit Labs"
    "Forge Systems"
    "Atlas Partners"
    "Beacon Works"
    "Crest Industries"
    "Delta Ventures"
    "Echo Solutions"
    "Falcon Tech"
    "Granite Ltd"
    "Harbour Co"
    "Ionic Media"
    "Jade Consulting"
    "Keystone Ltd"
    "Lumen Group"
    "Mosaic Corp"
    "Nexus Holdings"
    "Orbit Systems"
    "Prism Digital"
    "Quartz Labs"
    "Ridgeline Co"
    "Stellar Works"
    "Trident Corp"
    "Unity Partners"
    "Vanguard Ltd"
    "Wren Industries"
    "Zenith Group"
)

$Rng = [System.Random]::new()

# --- Random generators ---

function New-RandomDigits([int]$Length) {
    $result = ""
    for ($i = 0; $i -lt $Length; $i++) {
        $result += $Rng.Next(0, 10).ToString()
    }
    return $result
}

function New-RandomAlnum([int]$Length) {
    $charset = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
    $result = ""
    for ($i = 0; $i -lt $Length; $i++) {
        $result += $charset[$Rng.Next(0, $charset.Length)]
    }
    return $result
}

function New-RandomUuid {
    return [guid]::NewGuid().ToString()
}

function New-RandomSubmissionRef([string]$Original) {
    $groups = $Original -split '-'
    $parts = @()
    foreach ($group in $groups) {
        $parts += New-RandomAlnum $group.Length
    }
    return $parts -join '-'
}

function New-RandomBusinessName {
    return $BusinessNames[$Rng.Next(0, $BusinessNames.Count)]
}

# --- Pattern detection ---

function Get-Pattern([string]$Value) {
    if ($Value -match '^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$') {
        return "uuid"
    }
    if ($Value -match '^[A-Za-z0-9]{2,6}(-[A-Za-z0-9]{2,6})+$') {
        return "submission_ref"
    }
    if ($Value -match '^[0-9]{4,}$') {
        return "digits"
    }
    return "text"
}

function New-Replacement([string]$Original) {
    $pattern = Get-Pattern $Original
    switch ($pattern) {
        "uuid"           { return New-RandomUuid }
        "submission_ref" { return New-RandomSubmissionRef $Original }
        "digits"         { return New-RandomDigits $Original.Length }
        "text"           { return New-RandomBusinessName }
    }
}

# --- Main ---

if ($Count -lt 1) {
    Write-Error "Count must be a positive integer, got: $Count"
    exit 1
}

if (-not (Test-Path $SourceFile)) {
    Write-Error "Source file not found: $SourceFile"
    exit 1
}

$sourceContent = Get-Content -Path $SourceFile -Raw

# Detect patterns and show what we found
Write-Host "Source: $SourceFile"
Write-Host "Copies: $Count"
Write-Host ""
Write-Host "Values to replace:"

$submissionRefIdx = -1
for ($i = 0; $i -lt $Values.Count; $i++) {
    $pat = Get-Pattern $Values[$i]
    Write-Host ("  [{0}] `"{1}`" -> {2}" -f ($i + 1), $Values[$i], $pat)
    if ($pat -eq "submission_ref" -and $submissionRefIdx -eq -1) {
        $submissionRefIdx = $i
    }
}
Write-Host ""

# Sort indices by value length (longest first) to avoid substring collision
$sortedIndices = 0..($Values.Count - 1) | Sort-Object { $Values[$_].Length } -Descending

# Create output directory
if (-not (Test-Path $Output)) {
    New-Item -ItemType Directory -Path $Output -Force | Out-Null
}

# Verify all values exist in source
for ($i = 0; $i -lt $Values.Count; $i++) {
    if (-not $sourceContent.Contains($Values[$i])) {
        Write-Warning "Value not found in source: `"$($Values[$i])`""
    }
}

# Generate copies
Write-Host "Generating $Count files..."

for ($n = 1; $n -le $Count; $n++) {
    # Generate replacements for each value
    $replacements = @()
    for ($i = 0; $i -lt $Values.Count; $i++) {
        $replacements += New-Replacement $Values[$i]
    }

    # Determine output filename
    if ($submissionRefIdx -ge 0) {
        $outName = "$($replacements[$submissionRefIdx]).json"
    } else {
        $outName = "varied_{0:D3}.json" -f $n
    }

    # Replace longest strings first
    $content = $sourceContent
    foreach ($idx in $sortedIndices) {
        $content = $content.Replace($Values[$idx], $replacements[$idx])
    }

    # Write output file
    $outPath = Join-Path $Output $outName
    Set-Content -Path $outPath -Value $content -Encoding UTF8 -NoNewline

    Write-Host "  [$n/$Count] $outName"
}

Write-Host ""
Write-Host "Output: $Output/"
Write-Host "Done."
