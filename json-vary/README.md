# json-vary

Utilities for generating varied JSON test submissions with matching renamed attachments.

## Scripts

| Script | Platform | Description |
|--------|----------|-------------|
| `run-all.ps1` | PowerShell (Windows/macOS/Linux) | All-in-one: generates varied JSONs and copies renamed attachments |
| `json-vary.sh` | Bash (Linux/macOS) | Standalone JSON variation only (no attachment handling) |
| `json-vary.ps1` | PowerShell | Standalone JSON variation only (PowerShell port of json-vary.sh) |

## Quick Start (run-all.ps1)

```powershell
.\run-all.ps1 '9237766545' 'T9Q0-IIIB-PP52' 'a8d91e74-2285-4582-9d7c-fe6b400da347' 'SUA tec04'
```

This will:
1. Read each JSON template from `./templates/`
2. Generate 7 varied copies per template (with all four values replaced by random values of the same pattern)
3. Copy and rename attachments from `./attachments/projectTypeN/` for each generated submission reference
4. Write a master `manifest.csv` in the attachments output directory

### Parameters

| Parameter | Alias | Default | Description |
|-----------|-------|---------|-------------|
| (positional) | | *required* | Literal strings to find and replace |
| `-Variations` | `-n` | `7` | Number of varied JSON copies per template |
| `-MaxAttachments` | `-m` | `0` (all) | Max attachments per submission (0 = use all available) |
| `-TemplatesDir` | `-t` | `./templates` | Directory containing JSON template files |
| `-AttachmentsDir` | `-a` | `./attachments` | Directory containing per-projectType source attachments |
| `-Output` | `-o` | `./output` | Parent output directory |

### Examples

```powershell
# Default: 7 variations, all attachments
.\run-all.ps1 '9237766545' 'T9Q0-IIIB-PP52' 'a8d91e74-2285-4582-9d7c-fe6b400da347' 'SUA tec04'

# 10 variations, max 3 attachments each
.\run-all.ps1 -Variations 10 -MaxAttachments 3 '9237766545' 'T9Q0-IIIB-PP52' 'a8d91e74-2285-4582-9d7c-fe6b400da347' 'SUA tec04'

# Custom directories
.\run-all.ps1 -TemplatesDir ./my-templates -AttachmentsDir ./my-attachments -Output ./results '9237766545' 'T9Q0-IIIB-PP52'
```

### Output Structure

```
output/
  projectType1/           # Varied JSONs from template 1
    DYKN-ISSR-MMPZ.json
    UTXN-JB5W-WOP9.json
    ...
  projectType2/           # Varied JSONs from template 2
    A3MR-KQ4N-8PLJ.json
    ...
  projectType3/           # Varied JSONs from template 3
    ...
  attachments/            # All renamed attachments (flat)
    DYKNISSR MMPZ_1_API_Documentation.pdf
    DYKNISSRMMPZ_2_Sprint_Burndown.jpeg
    ...
    manifest.csv          # Master manifest (mail_item_id, attached_id, filename)
```

The script is **idempotent** -- it cleans the output directory before each run. Source templates and attachments are never modified.

### Directory Layout

```
json-vary/
  run-all.ps1             # Main script
  templates/              # JSON templates (1 per project type)
    gForm-template-1.json
    gForm-template-2.json
    gForm-template-3.json
  attachments/            # Source attachments (1 dir per project type)
    projectType1/
      ALLFORMAT123_1_API_Documentation.pdf
      ALLFORMAT123_2_Sprint_Burndown.jpeg
      ...
    projectType2/
      ...
    projectType3/
      ...
```

Templates are matched to attachment directories by position: the first template (alphabetically) maps to `projectType1`, the second to `projectType2`, etc.

## Pattern Detection

Values passed on the command line are auto-detected by format:

| Pattern | Example | Replacement |
|---------|---------|-------------|
| All digits (4+ chars) | `9237766545` | Random digits, same length |
| Alphanumeric groups with hyphens | `T9Q0-IIIB-PP52` | Random alphanumeric groups, same structure |
| UUID (8-4-4-4-12 hex) | `a8d91e74-2285-4582-9d7c-fe6b400da347` | Random UUID |
| Anything else | `SUA tec04` | Random business name from built-in list |

Replacements are applied longest-first to avoid substring collisions. Output JSON files are named after the generated submission reference (e.g., `DYKN-ISSR-MMPZ.json`). Attachment filenames use the same reference with hyphens removed (e.g., `DYKNISSRMMPZ_1_API_Documentation.pdf`).

## Standalone json-vary Scripts

For generating varied JSONs without the attachment-copying step:

### Bash (json-vary.sh)

```bash
./json-vary.sh [-o <dir>] <source.json> <count> <value1> [value2] ...
./json-vary.sh -h
```

Runs on Linux and macOS. Uses only `sed`, `od`, `tr`, `/dev/urandom` -- no Python, no `uuidgen`. Suitable for AWS Linux instances.

### PowerShell (json-vary.ps1)

```powershell
.\json-vary.ps1 [-Output <dir>] <source.json> <count> <value1> [value2] ...
Get-Help .\json-vary.ps1 -Detailed
```

## Notes

- If no submission-reference pattern is provided, files are named `varied_001.json`, etc.
- The manifest CSV format matches the Java fake-data tool: `mail_item_id,attached_id,filename`
- IDs are 16-character lowercase alphanumeric strings
