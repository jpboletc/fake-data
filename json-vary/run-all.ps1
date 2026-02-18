<#
.SYNOPSIS
    Generate varied JSON submissions and matching renamed attachments.

.DESCRIPTION
    For each JSON template in the templates directory, generates N varied
    copies with specified literal strings replaced by pattern-matched random
    values. Then copies and renames attachments for each generated submission
    reference, producing a master manifest CSV.

    Pattern detection (automatic from value format):
      - All digits (4+ chars)           -> random digits, same length
      - XXXX-XXXX-XXXX (alphanumeric)   -> random alphanumeric groups
      - UUID (8-4-4-4-12 hex)           -> random UUID
      - Anything else                   -> random business name

    Idempotent: cleans the output directory before each run.

.PARAMETER Values
    Literal strings to find and replace in each template.

.PARAMETER TemplatesDir
    Directory containing JSON template files (default: ./templates).

.PARAMETER AttachmentsDir
    Directory containing per-projectType attachment files (default: ./attachments).

.PARAMETER Variations
    Number of varied JSON copies per template (default: 7).

.PARAMETER MaxAttachments
    Maximum number of attachments per submission reference.
    0 = use all available attachments (default: 0).

.PARAMETER Output
    Parent output directory (default: ./output).

.EXAMPLE
    .\run-all.ps1 '9237766545' 'T9Q0-IIIB-PP52' 'a8d91e74-2285-4582-9d7c-fe6b400da347' 'SUA tec04'

.EXAMPLE
    .\run-all.ps1 -Variations 10 -MaxAttachments 3 -Output ./results '9237766545' 'T9Q0-IIIB-PP52'
#>

[CmdletBinding(PositionalBinding=$false)]
param(
    [Parameter(Mandatory=$true, ValueFromRemainingArguments=$true)]
    [string[]]$Values,

    [Alias("t")]
    [string]$TemplatesDir = "./templates",

    [Alias("a")]
    [string]$AttachmentsDir = "./attachments",

    [Alias("n")]
    [int]$Variations = 7,

    [Alias("m")]
    [int]$MaxAttachments = 0,

    [Alias("o")]
    [string]$Output = "./output"
)

$ErrorActionPreference = "Stop"

# ============================================================
# Random generators
# ============================================================

$Rng = [System.Random]::new()

$BusinessNames = @(
    "Apex Holdings",  "Nova Digital",    "Vertex Group",    "Pinnacle Corp"
    "Summit Labs",    "Forge Systems",   "Atlas Partners",  "Beacon Works"
    "Crest Industries","Delta Ventures", "Echo Solutions",  "Falcon Tech"
    "Granite Ltd",    "Harbour Co",      "Ionic Media",     "Jade Consulting"
    "Keystone Ltd",   "Lumen Group",     "Mosaic Corp",     "Nexus Holdings"
    "Orbit Systems",  "Prism Digital",   "Quartz Labs",     "Ridgeline Co"
    "Stellar Works",  "Trident Corp",    "Unity Partners",  "Vanguard Ltd"
    "Wren Industries","Zenith Group"
)

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

function New-ManifestId {
    $charset = "abcdefghijklmnopqrstuvwxyz0123456789"
    $result = ""
    for ($i = 0; $i -lt 16; $i++) {
        $result += $charset[$Rng.Next(0, $charset.Length)]
    }
    return $result
}

# ============================================================
# Pattern detection and replacement
# ============================================================

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

# ============================================================
# Validation
# ============================================================

if (-not (Test-Path $TemplatesDir)) {
    Write-Error "Templates directory not found: $TemplatesDir"
    exit 1
}

$templates = Get-ChildItem -Path $TemplatesDir -Filter "*.json" | Sort-Object Name

if ($templates.Count -eq 0) {
    Write-Error "No JSON files found in: $TemplatesDir"
    exit 1
}

if ($Variations -lt 1) {
    Write-Error "Variations must be a positive integer, got: $Variations"
    exit 1
}

# Safety check: never delete the source attachments dir
$resolvedOutput = (Resolve-Path -Path $Output -ErrorAction SilentlyContinue).Path ?? (Join-Path (Get-Location) $Output)
$resolvedAttach = (Resolve-Path -Path $AttachmentsDir -ErrorAction SilentlyContinue).Path ?? (Join-Path (Get-Location) $AttachmentsDir)
if ($resolvedOutput -eq $resolvedAttach) {
    Write-Error "Output directory cannot be the same as attachments directory"
    exit 1
}

# ============================================================
# Prepare replacement values
# ============================================================

$submissionRefIdx = -1
for ($i = 0; $i -lt $Values.Count; $i++) {
    $pat = Get-Pattern $Values[$i]
    if ($pat -eq "submission_ref" -and $submissionRefIdx -eq -1) {
        $submissionRefIdx = $i
    }
}

# Sort indices by value length (longest first) to avoid substring collision
$sortedIndices = 0..($Values.Count - 1) | Sort-Object { $Values[$_].Length } -Descending

# ============================================================
# Summary
# ============================================================

Write-Host "Templates:       $TemplatesDir ($($templates.Count) files)"
Write-Host "Attachments:     $AttachmentsDir"
Write-Host "Variations:      $Variations per template"
if ($MaxAttachments -gt 0) {
    Write-Host "Max attachments: $MaxAttachments per submission"
} else {
    Write-Host "Max attachments: all available"
}
Write-Host "Output:          $Output"
Write-Host ""
Write-Host "Values to replace:"
for ($i = 0; $i -lt $Values.Count; $i++) {
    $pat = Get-Pattern $Values[$i]
    Write-Host ("  [{0}] `"{1}`" -> {2}" -f ($i + 1), $Values[$i], $pat)
}
Write-Host ""

# ============================================================
# Clean output directory for idempotent runs
# ============================================================

if (Test-Path $Output) {
    Write-Host "Cleaning previous output: $Output"
    Remove-Item -Path $Output -Recurse -Force
}

# ============================================================
# Phase 1: Generate varied JSON files
# ============================================================

Write-Host "--- Phase 1: Generating varied JSON files ---"
Write-Host ""

$templateNum = 1
foreach ($template in $templates) {
    $templateTag = [System.IO.Path]::GetFileNameWithoutExtension($template.Name)
    $outDir = Join-Path $Output $templateTag
    New-Item -ItemType Directory -Path $outDir -Force | Out-Null

    $sourceContent = Get-Content -Path $template.FullName -Raw

    # Verify all values exist in this template
    for ($i = 0; $i -lt $Values.Count; $i++) {
        if (-not $sourceContent.Contains($Values[$i])) {
            Write-Warning "Value not found in $($template.Name): `"$($Values[$i])`""
        }
    }

    Write-Host "=== [$templateNum/$($templates.Count)] $($template.Name) -> $templateTag ==="

    for ($n = 1; $n -le $Variations; $n++) {
        # Generate replacements
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
        $outPath = Join-Path $outDir $outName
        Set-Content -Path $outPath -Value $content -Encoding UTF8 -NoNewline

        Write-Host "  [$n/$Variations] $outName"
    }

    Write-Host ""
    $templateNum++
}

# ============================================================
# Phase 2: Copy and rename attachments
# ============================================================

Write-Host "--- Phase 2: Copying attachments ---"
Write-Host ""

$attachmentsOutDir = Join-Path $Output "attachments"
New-Item -ItemType Directory -Path $attachmentsOutDir -Force | Out-Null

# Write manifest header
$manifestPath = Join-Path $attachmentsOutDir "manifest.csv"
"mail_item_id,attached_id,filename" | Set-Content -Path $manifestPath -Encoding UTF8

$totalCopied = 0
foreach ($template in $templates) {
    $templateTag = [System.IO.Path]::GetFileNameWithoutExtension($template.Name)
    $jsonDir = Join-Path $Output $templateTag
    $srcAttachDir = Join-Path $AttachmentsDir $templateTag

    if (-not (Test-Path $srcAttachDir)) {
        Write-Warning "No attachments directory for '$templateTag' at $srcAttachDir - skipping"
        continue
    }

    # Get the source attachment files (exclude manifest CSVs)
    $sourceAttachments = Get-ChildItem -Path $srcAttachDir -File | Where-Object { $_.Extension -ne ".csv" }

    if ($sourceAttachments.Count -eq 0) {
        Write-Warning "No attachment files found in $srcAttachDir"
        continue
    }

    # Apply max attachments limit
    if ($MaxAttachments -gt 0 -and $sourceAttachments.Count -gt $MaxAttachments) {
        $sourceAttachments = $sourceAttachments | Select-Object -First $MaxAttachments
    }

    # Detect the common prefix from the first attachment filename
    $firstFile = $sourceAttachments[0].Name
    if ($firstFile -match '^([^_]+)_') {
        $originalPrefix = $Matches[1]
    } else {
        Write-Warning "Cannot detect prefix from '$firstFile' - skipping $templateTag"
        continue
    }

    # Get generated JSON files for this template
    $jsonFiles = Get-ChildItem -Path $jsonDir -Filter "*.json" -ErrorAction SilentlyContinue

    if ($null -eq $jsonFiles -or $jsonFiles.Count -eq 0) {
        Write-Warning "No JSON files found in $jsonDir"
        continue
    }

    Write-Host "=== ${templateTag}: $($jsonFiles.Count) submissions x $($sourceAttachments.Count) attachments (prefix: ${originalPrefix}) ==="

    foreach ($jsonFile in $jsonFiles) {
        # Submission reference is the filename without .json
        $submissionRef = [System.IO.Path]::GetFileNameWithoutExtension($jsonFile.Name)
        # Remove hyphens for use as filename prefix
        $filePrefix = $submissionRef -replace '-', ''

        foreach ($attachment in $sourceAttachments) {
            $newName = $attachment.Name -replace [regex]::Escape($originalPrefix), $filePrefix
            $destPath = Join-Path $attachmentsOutDir $newName
            Copy-Item -Path $attachment.FullName -Destination $destPath

            $mailItemId = New-ManifestId
            $attachedId = New-ManifestId
            "${mailItemId},${attachedId},${newName}" | Add-Content -Path $manifestPath -Encoding UTF8

            $totalCopied++
        }
        Write-Host "  $submissionRef -> $($sourceAttachments.Count) files"
    }

    Write-Host ""
}

# ============================================================
# Summary
# ============================================================

Write-Host "Attachments: $attachmentsOutDir/"
Write-Host "Manifest:    $manifestPath"
Write-Host "Total files: $totalCopied attachments"
Write-Host ""
Write-Host "All done. $($templates.Count) templates x $Variations variations = $($templates.Count * $Variations) submissions."
