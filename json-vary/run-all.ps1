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

.PARAMETER StartHour
    Starting hour (0-23) for manifest timestamps (default: current hour).

.PARAMETER Manifests
    Number of manifest files to create (default: 1).
    Attachments are randomly distributed across manifests.
    Hours increment from StartHour; dates advance on midnight rollover.

.EXAMPLE
    .\run-all.ps1 '9237766545' 'T9Q0-IIIB-PP52' 'a8d91e74-2285-4582-9d7c-fe6b400da347' 'SUA tec04'

.EXAMPLE
    .\run-all.ps1 -Variations 10 -MaxAttachments 3 -Output ./results '9237766545' 'T9Q0-IIIB-PP52'

.EXAMPLE
    .\run-all.ps1 -Manifests 3 -StartHour 15 '9237766545' 'T9Q0-IIIB-PP52' 'a8d91e74-2285-4582-9d7c-fe6b400da347' 'SUA tec04'
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
    [string]$Output = "./output",

    [Alias("H")]
    [int]$StartHour = -1,

    [int]$Manifests = 1
)

$ErrorActionPreference = "Stop"

# ============================================================
# Random generators
# ============================================================

$Rng = [System.Random]::new()

# Company name word lists (100 each) - combined to generate thousands of unique names
$NameAdjectives = @(
    "Blue",       "Golden",     "Silver",     "Pacific",    "Northern",   "Southern",   "Atlantic",   "Alpine",     "Coastal",    "Sterling"
    "Amber",      "Crimson",    "Emerald",    "Sapphire",   "Cedar",      "Maple",      "Iron",       "Copper",     "Granite",    "Crystal"
    "Coral",      "Arctic",     "Solar",      "Lunar",      "Stellar",    "Cascade",    "Pinnacle",   "Meridian",   "Horizon",    "Frontier"
    "Heritage",   "Legacy",     "Pioneer",    "Eagle",      "Falcon",     "Phoenix",    "Sentinel",   "Titan",      "Aurora",     "Nova"
    "Quantum",    "Vertex",     "Prism",      "Fusion",     "Catalyst",   "Beacon",     "Compass",    "Anchor",     "Keystone",   "Landmark"
    "Swift",      "Agile",      "Dynamic",    "Elite",      "Noble",      "Bright",     "Vital",      "Allied",     "United",     "Metro"
    "Continental","Premier",    "Prime",      "Royal",      "Grand",      "Summit",     "Crest",      "Aspen",      "Birch",      "Elm"
    "Oak",        "Pine",       "Sage",       "Flint",      "Onyx",       "Cobalt",     "Scarlet",    "Indigo",     "Azure",      "Polaris"
    "Orion",      "Triton",     "Vega",       "Zenith",     "Apex",       "Evergreen",  "Redwood",    "Sequoia",    "Sierra",     "Boreal"
    "Tidal",      "Radiant",    "Integral",   "Resolute",   "Sovereign",  "Astra",      "Trident",    "Central",    "Civic",      "Global"
)

$NameNouns = @(
    "Ridge",      "Harbor",     "Creek",      "Valley",     "Stone",      "Field",      "Grove",      "Bridge",     "Gate",       "Tower"
    "Park",       "Bay",        "Cove",       "Hill",       "Glen",       "Brook",      "Forge",      "River",      "Cliff",      "Point"
    "Landing",    "Crossing",   "Dale",       "Haven",      "Shore",      "Rock",       "Peak",       "Canyon",     "Mesa",       "Trail"
    "Vista",      "Hollow",     "Ledge",      "Basin",      "Arch",       "Pier",       "Coast",      "Port",       "Inlet",      "Cape"
    "Terrace",    "Knoll",      "Spur",       "Bend",       "Crown",      "Shield",     "Spire",      "Spring",     "Lake",       "Marsh"
    "Bluff",      "Prairie",    "Glade",      "Dune",       "Range",      "Channel",    "Strand",     "Reef",       "Oasis",      "Plateau"
    "Heath",      "Moor",       "Dell",       "Trace",      "Edge",       "Bond",       "Line",       "Core",       "Link",       "Nexus"
    "Pulse",      "Spark",      "Wave",       "Tide",       "Axis",       "Orbit",      "Lens",       "Venture",    "Capital",    "Croft"
    "Wells",      "Gardens",    "Meadow",     "Commons",    "Quarter",    "Circuit",    "Loop",       "Reach",      "Pass",       "Strait"
    "Pointe",     "Pines",      "Sands",      "Springs",    "Heights",    "Narrows",    "Rapids",     "Falls",      "Harbour",    "Wharf"
)

$NameSuffixes = @(
    "Holdings",   "Group",      "Partners",   "Corp",       "Industries", "Solutions",  "Systems",    "Technologies","Dynamics",   "Ventures"
    "Capital",    "Consulting", "Enterprises","Associates", "Services",   "Analytics",  "Advisors",   "Global",     "International","Networks"
    "Digital",    "Labs",       "Works",      "Co",         "Ltd",        "Inc",        "Media",      "Energy",     "Logistics",  "Supply"
    "Direct",     "Alliance",   "Collective", "Foundation", "Institute",  "Agency",     "Trust",      "Fund",       "Exchange",   "Trade"
    "Commerce",   "Properties", "Development","Design",     "Creative",   "Studio",     "Research",   "Resources",  "Materials",  "Products"
    "Financial",  "Health",     "Scientific", "Engineering","Manufacturing","Biotech",  "Software",   "Intelligence","Security",  "Communications"
    "Aerospace",  "Marine",     "Automotive", "Pharma",     "Medical",    "Electronics","Robotics",   "Strategies", "Advisory",   "Management"
    "Freight",    "Transport",  "Aviation",   "Defence",    "Power",      "Mining",     "Agriculture","Construction","Realty",     "Telecom"
    "Wireless",   "Cloud",      "Data",       "Hardware",   "Textiles",   "Metals",     "Minerals",   "Nutrition",  "Foods",      "Chemicals"
    "Optics",     "Devices",    "Components", "Instruments","Brands",     "Concepts",   "Express",    "Connect",    "Source",     "Platform"
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
    $adj = $NameAdjectives[$Rng.Next(0, $NameAdjectives.Count)]
    $noun = $NameNouns[$Rng.Next(0, $NameNouns.Count)]
    $suffix = $NameSuffixes[$Rng.Next(0, $NameSuffixes.Count)]
    $pattern = $Rng.Next(0, 5)
    switch ($pattern) {
        { $_ -le 2 } { return "$adj $noun $suffix" }   # 60%: Pacific Ridge Holdings
        3             { return "$noun $suffix" }          # 20%: Meridian Corp
        4             { return "$adj $suffix" }           # 20%: Sterling Partners
    }
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
$rp = Resolve-Path -Path $Output -ErrorAction SilentlyContinue
$resolvedOutput = if ($rp) { $rp.Path } else { Join-Path (Get-Location) $Output }
$rp = Resolve-Path -Path $AttachmentsDir -ErrorAction SilentlyContinue
$resolvedAttach = if ($rp) { $rp.Path } else { Join-Path (Get-Location) $AttachmentsDir }
if ($resolvedOutput -eq $resolvedAttach) {
    Write-Error "Output directory cannot be the same as attachments directory"
    exit 1
}

if ($Manifests -lt 1) {
    Write-Error "Manifests must be a positive integer, got: $Manifests"
    exit 1
}

if ($StartHour -eq -1) {
    $StartHour = (Get-Date).Hour
} elseif ($StartHour -lt 0 -or $StartHour -gt 23) {
    Write-Error "StartHour must be 0-23, got: $StartHour"
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
if ($Manifests -gt 1) {
    Write-Host "Manifests:       $Manifests (starting at hour $('{0:D2}' -f $StartHour))"
}
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

$attachmentsOutDir = (New-Item -ItemType Directory -Path (Join-Path $Output "attachments") -Force).FullName

# Create manifest file(s) with empty first line, no header
# Use UTF-8 without BOM (PS 5.1's -Encoding UTF8 adds a BOM which breaks Java CSV parsing)
$utf8NoBom = New-Object System.Text.UTF8Encoding($false)
$baseDate = Get-Date
$manifestPaths = @()
for ($mi = 0; $mi -lt $Manifests; $mi++) {
    $ts = $baseDate.Date.AddHours($StartHour + $mi)
    $stamp = $ts.ToString("ddMMyyHH")
    $path = Join-Path $attachmentsOutDir "manifest${stamp}.csv"
    [System.IO.File]::WriteAllText($path, "`r`n", $utf8NoBom)
    $manifestPaths += $path
}

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
            $chosenManifest = $manifestPaths[$Rng.Next(0, $manifestPaths.Count)]
            [System.IO.File]::AppendAllText($chosenManifest, "${mailItemId},${attachedId},${newName}`r`n", $utf8NoBom)

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
if ($manifestPaths.Count -eq 1) {
    Write-Host "Manifest:    $($manifestPaths[0])"
} else {
    Write-Host "Manifests:   $($manifestPaths.Count) files"
    foreach ($mp in $manifestPaths) {
        Write-Host "             $(Split-Path $mp -Leaf)"
    }
}
Write-Host "Total files: $totalCopied attachments"
Write-Host ""
Write-Host "All done. $($templates.Count) templates x $Variations variations = $($templates.Count * $Variations) submissions."
