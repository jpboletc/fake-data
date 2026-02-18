# json-vary

Utilities for generating varied JSON test submissions with matching renamed attachments.

## Scripts

| Script | Platform | Description |
|--------|----------|-------------|
| `run-all.sh` | Bash (Linux/macOS) | All-in-one: generates varied JSONs and copies renamed attachments |
| `run-all.ps1` | PowerShell (Windows/macOS/Linux) | All-in-one: same as above, PowerShell version |
| `post-gforms.ps1` | PowerShell | POST generated gForm JSONs one-by-one to an endpoint |
| `post-attachments.ps1` | PowerShell | POST all generated attachments in a single multipart request |
| `json-vary.sh` | Bash (Linux/macOS) | Standalone JSON variation only (no attachment handling) |
| `json-vary.ps1` | PowerShell | Standalone JSON variation only (PowerShell port of json-vary.sh) |

## Quick Start

### Bash (run-all.sh)

```bash
./run-all.sh '9237766545' 'T9Q0-IIIB-PP52' 'a8d91e74-2285-4582-9d7c-fe6b400da347' 'SUA tec04'
```

### PowerShell (run-all.ps1)

```powershell
.\run-all.ps1 '9237766545' 'T9Q0-IIIB-PP52' 'a8d91e74-2285-4582-9d7c-fe6b400da347' 'SUA tec04'
```

This will:
1. Read each JSON template from `./templates/`
2. Generate 7 varied copies per template (with all four values replaced by random values of the same pattern)
3. Copy and rename attachments from `./attachments/<templateName>/` for each generated submission reference
4. Write a master `manifest.csv` in the attachments output directory

### Parameters

| Bash flag | PowerShell flag | Default | Description |
|-----------|-----------------|---------|-------------|
| (positional) | (positional) | *required* | Literal strings to find and replace |
| `-n`, `--variations` | `-Variations` (`-n`) | `7` | Number of varied JSON copies per template |
| `-m`, `--max-attachments` | `-MaxAttachments` (`-m`) | `0` (all) | Max attachments per submission (0 = use all available) |
| `-t`, `--templates` | `-TemplatesDir` (`-t`) | `./templates` | Directory containing JSON template files |
| `-a`, `--attachments` | `-AttachmentsDir` (`-a`) | `./attachments` | Directory containing per-template source attachments |
| `-o`, `--output` | `-Output` (`-o`) | `./output` | Parent output directory |

### Examples

```bash
# Default: 7 variations, all attachments
./run-all.sh '9237766545' 'T9Q0-IIIB-PP52' 'a8d91e74-2285-4582-9d7c-fe6b400da347' 'SUA tec04'

# 10 variations, max 3 attachments each
./run-all.sh -n 10 -m 3 '9237766545' 'T9Q0-IIIB-PP52' 'a8d91e74-2285-4582-9d7c-fe6b400da347' 'SUA tec04'

# Custom directories
./run-all.sh -t ./my-templates -a ./my-attachments -o ./results '9237766545' 'T9Q0-IIIB-PP52'
```

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
  gForm-template-1/       # Varied JSONs from template 1
    DYKN-ISSR-MMPZ.json
    UTXN-JB5W-WOP9.json
    ...
  gForm-template-2/       # Varied JSONs from template 2
    A3MR-KQ4N-8PLJ.json
    ...
  gForm-template-3/       # Varied JSONs from template 3
    ...
  attachments/            # All renamed attachments (flat)
    DYKNISSRMMPZ_1_API_Documentation.pdf
    DYKNISSRMMPZ_2_Sprint_Burndown.jpeg
    ...
    manifest.csv          # Master manifest (mail_item_id, attached_id, filename)
```

The script is **idempotent** -- it cleans the output directory before each run. Source templates and attachments are never modified.

### Directory Layout

```
json-vary/
  run-all.sh              # Main script (Bash)
  run-all.ps1             # Main script (PowerShell)
  templates/              # JSON templates (1 per project type)
    gForm-template-1.json
    gForm-template-2.json
    gForm-template-3.json
  attachments/            # Source attachments (1 dir per template)
    gForm-template-1/
      ALLFORMAT123_1_API_Documentation.pdf
      ALLFORMAT123_2_Sprint_Burndown.jpeg
      ...
    gForm-template-2/
      ...
    gForm-template-3/
      ...
```

Templates are matched to attachment directories by name: `templates/gForm-template-1.json` maps to `attachments/gForm-template-1/`.

## Pattern Detection

Values passed on the command line are auto-detected by format:

| Pattern | Example | Replacement |
|---------|---------|-------------|
| All digits (4+ chars) | `9237766545` | Random digits, same length |
| Alphanumeric groups with hyphens | `T9Q0-IIIB-PP52` | Random alphanumeric groups, same structure |
| UUID (8-4-4-4-12 hex) | `a8d91e74-2285-4582-9d7c-fe6b400da347` | Random UUID |
| Anything else | `SUA tec04` | Random business name from built-in list |

Replacements are applied longest-first to avoid substring collisions. Output JSON files are named after the generated submission reference (e.g., `DYKN-ISSR-MMPZ.json`). Attachment filenames use the same reference with hyphens removed (e.g., `DYKNISSRMMPZ_1_API_Documentation.pdf`).

## Uploading to Endpoints

After generating varied JSONs and attachments with `run-all`, use the upload scripts to POST them to your service endpoints. These scripts replicate the exact multipart/form-data request structure used by the browser (captured from Swagger UI / Chrome DevTools), including browser-like headers and Basic authentication.

### post-gforms.ps1 -- POST gForm JSONs

Iterates all `*.json` files under the given directory (recursively) and POSTs each individually as a multipart file upload.

```powershell
.\post-gforms.ps1 -Uri 'https://localhost:8080/api/source-files/upload-in-file' `
    -AuthToken 'Basic On53YF0/fGA4SS9JQCUlIX51MEUq' `
    -JsonDir './output'
```

| Parameter | Description |
|-----------|-------------|
| `-Uri` | Endpoint URL to POST each JSON file to |
| `-AuthToken` | Full Basic auth string (e.g., `Basic XXXXX`) |
| `-JsonDir` | Root directory containing JSON subdirectories (searched recursively) |
| `-SkipCertCheck` | Skip SSL certificate validation (for localhost / self-signed certs) |

Each file is sent as `multipart/form-data` with field name `file`. The script reports per-file success/failure with HTTP status codes and prints a summary at the end.

### post-attachments.ps1 -- POST attachments

Collects all non-CSV files from the attachments directory and sends them in a single multipart/form-data POST request.

```powershell
.\post-attachments.ps1 -Uri 'https://localhost:8080/api/cavr/attachments/s3files' `
    -AuthToken 'Basic On53YF0/fGA4SS9JQCUlIX51MEUq' `
    -AttachmentsDir './output/attachments'
```

| Parameter | Description |
|-----------|-------------|
| `-Uri` | Endpoint URL to POST attachments to |
| `-AuthToken` | Full Basic auth string (e.g., `Basic XXXXX`) |
| `-AttachmentsDir` | Directory containing attachment files to upload |
| `-SkipCertCheck` | Skip SSL certificate validation (for localhost / self-signed certs) |

All files are sent in one request, each as a separate part with field name `files`. Content-types are detected from file extensions (PDF, JPEG, XLSX, DOCX, PPTX, ODS, ODT, ODP, etc.). The script reads all files into memory, so be mindful of total attachment size vs. server upload limits.

### Typical Workflow

```powershell
# 1. Generate varied JSONs and attachments
.\run-all.ps1 '9237766545' 'T9Q0-IIIB-PP52' 'a8d91e74-2285-4582-9d7c-fe6b400da347' 'SUA tec04'

# 2. POST gForm JSONs to the source-files endpoint
.\post-gforms.ps1 -Uri 'https://myserver/api/source-files/upload-in-file' `
    -AuthToken 'Basic dXNlcjpwYXNz' -JsonDir './output'

# 3. POST attachments to the attachments endpoint
.\post-attachments.ps1 -Uri 'https://myserver/api/cavr/attachments/s3files' `
    -AuthToken 'Basic dXNlcjpwYXNz' -AttachmentsDir './output/attachments'
```

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
