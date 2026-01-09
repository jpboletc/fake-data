# Fake Data Generator

A command-line utility that generates realistic dummy attachment files with themed content and a CSV manifest. Supports multiple file formats and industry-specific themes.

## Features

- **9 File Formats**: PDF, JPEG, XLSX, XLS, ODS, DOCX, ODT, PPTX, ODP
- **7 Industry Themes**: Financial, Entertainment, Healthcare, Technology, Legal, Education, Retail
- **Realistic Content**: Documents contain contextually appropriate data, not just lorem ipsum
- **CSV Manifest**: Tracks all generated files with unique IDs
- **Flexible Input**: Submission references via CLI or file (with per-line themes)
- **Self-contained Bundles**: jlink-based distribution with bundled JRE (no Java installation required)

## Installation

### Option 1: Download Pre-built Bundle

Download the appropriate bundle for your platform from the [Releases](../../releases) page:
- `fake-data-linux.zip` - Linux (x64)
- `fake-data-macos-arm64.zip` - macOS (Apple Silicon)
- `fake-data-macos-x64.zip` - macOS (Intel)
- `fake-data-windows.zip` - Windows

Each bundle includes a minimal JRE, so no Java installation is required.

```bash
# Extract and run (Unix)
unzip fake-data-linux.zip
./fake-data/bin/fake-data --help

# Windows
# Extract the zip and run fake-data\bin\fake-data.bat
```

### Option 2: Download Fat JAR

If you already have Java 21+ installed, download `fake-data-all.jar` from the [Releases](../../releases) page:

```bash
java -jar fake-data-all.jar --help
```

### Option 3: Build from Source

Requirements:
- Java 21+
- Gradle 8.10+ (or use the included wrapper)

```bash
# Build the fat JAR
./gradlew shadowJar

# Run with Java
java -jar build/libs/fake-data-all.jar --help
```

### Option 4: Build Self-contained Bundle (jlink)

Build a self-contained bundle with a minimal JRE for your current platform:

```bash
# Build jlink image
./gradlew jlinkZip

# Output location (platform-specific)
# Linux: build/distributions/fake-data-linux.zip
# macOS ARM: build/distributions/fake-data-macos-arm64.zip
# macOS Intel: build/distributions/fake-data-macos-x64.zip
# Windows: build/distributions/fake-data-windows.zip
```

## Usage

### Basic Usage

```bash
# Generate files for submission references (using jlink bundle)
./fake-data/bin/fake-data --refs "QY76OVC07S34,BM76OVC07S34" \
  --formats "pdf:2,xlsx:1,pptx:1" \
  --output ./output

# Using the JAR
java -jar fake-data-all.jar --refs "QY76OVC07S34" --formats "pdf:1"
```

### CLI Options

| Option | Short | Description | Default |
|--------|-------|-------------|---------|
| `--refs` | `-r` | Comma-separated submission references | - |
| `--file` | `-f` | File with submission references (one per line) | - |
| `--formats` | `-F` | Format specification `format:count,...` | `pdf:1,xlsx:1,docx:1,pptx:1` |
| `--output` | `-o` | Output directory | `./output` |
| `--pattern` | `-p` | Regex for submission reference validation | `^[A-Za-z0-9]{12}$` |
| `--theme` | `-t` | Global content theme | `default` |
| `--help` | `-h` | Show help | - |
| `--version` | `-V` | Show version | - |

### Format Specification

Supported formats: `pdf`, `jpeg`, `xlsx`, `xls`, `ods`, `docx`, `odt`, `pptx`, `odp`

```bash
# 2 PDFs, 1 Excel, 1 PowerPoint per submission
--formats "pdf:2,xlsx:1,pptx:1"

# Just PDFs (count defaults to 1)
--formats "pdf"

# All supported formats
--formats "pdf:1,jpeg:1,xlsx:1,xls:1,ods:1,docx:1,odt:1,pptx:1,odp:1"

# Full example with all formats
./fake-data/bin/fake-data --refs "ALLFORMAT123" \
  --formats "pdf:1,jpeg:1,xlsx:1,xls:1,ods:1,docx:1,odt:1,pptx:1,odp:1" \
  --theme "technology" \
  --output ./all-formats-output
```

### Themes

Available themes customize document names, content, and terminology:

| Theme | Description |
|-------|-------------|
| `financial` | Banking, investment, accounting content |
| `entertainment` | Media, production, streaming content |
| `healthcare` | Medical, clinical, patient care content |
| `technology` | Software, cloud, infrastructure content |
| `legal` | Law firm, litigation, contract content |
| `education` | Academic, research, enrollment content |
| `retail` | Store operations, inventory, sales content |
| `default` | Generic business content |

```bash
# Global theme for all submissions
./fake-data/bin/fake-data --refs "REF123456789" --theme "financial" --formats "pdf:1"
```

### File Input with Per-Submission Themes

Create a text file with one submission reference per line. Add `// theme` comments for per-submission theming:

```text
# submissions.txt
AJKD1234OMJU // financial
KHSBS9877BNS // entertainment & media
TECH12345678 // technology
GENERIC12345
```

```bash
./fake-data/bin/fake-data --file submissions.txt --formats "pdf:2,xlsx:1"
```

Output:
```
Processing: AJKD1234OMJU [FINANCIAL]
  Created: AJKD1234OMJU_1_Risk_Assessment.pdf
  Created: AJKD1234OMJU_2_Asset_Valuation.xlsx
Processing: KHSBS9877BNS [ENTERTAINMENT]
  Created: KHSBS9877BNS_1_Production_Schedule.pdf
  Created: KHSBS9877BNS_2_Royalty_Calculations.xlsx
...
```

### Custom Validation Pattern

Override the default 12-character alphanumeric pattern:

```bash
# 8-character uppercase only
./fake-data/bin/fake-data --refs "ABCD1234" --pattern "^[A-Z0-9]{8}$" --formats "pdf:1"

# Any length alphanumeric with dashes
./fake-data/bin/fake-data --refs "REF-2024-001" --pattern "^[A-Za-z0-9-]+$" --formats "pdf:1"
```

## Output

### Generated Files

Files are named: `{submission_ref}_{number}_{document_name}.{ext}`

Example output directory:
```
output/
├── QY76OVC07S34_1_Quarterly_Financial_Report.pdf
├── QY76OVC07S34_2_Budget_Projections.xlsx
├── QY76OVC07S34_3_Investment_Memo.docx
├── QY76OVC07S34_4_Investor_Presentation.pptx
├── QY76OVC07S34_5_Portfolio_Allocation_Chart.jpeg
├── BM76OVC07S34_1_Annual_Audit_Report.pdf
├── BM76OVC07S34_2_Cash_Flow_Model.xlsx
└── manifest.csv
```

### Manifest CSV

The manifest tracks all generated files with unique identifiers:

```csv
mail_item_id,attached_id,filename
v7939zmnf9xcnzuy,fkh4uqz8q1g8eg5s,QY76OVC07S34_1_Tax_Filing_Summary.pdf
9pvkcqbf4llae8xm,ak2sf46rsd1dzrw5,QY76OVC07S34_2_Revenue_Forecast.xlsx
w41ohrzh96f7r86b,0mfs7vg26m4wq4fu,QY76OVC07S34_3_Financial_Procedures.docx
```

- `mail_item_id`: 16-character random alphanumeric identifier
- `attached_id`: 16-character random alphanumeric identifier
- `filename`: Full filename with submission reference prefix

## Technology Stack

- **Language**: Java 21
- **Build**: Gradle 8.10 with Shadow and jlink plugins
- **CLI**: Picocli
- **Fake Data**: Datafaker
- **PDF**: OpenPDF (with full table and color support)
- **Office (DOCX/XLSX/PPTX)**: Apache POI
- **OpenDocument (ODT/ODS/ODP)**: ODF Toolkit (Simple ODF)
- **Images**: Java2D (bar charts, pie charts, line graphs)
- **CSV**: OpenCSV
- **Distribution**: jlink for self-contained bundles with minimal JRE

## Building for Distribution

### GitHub Actions

The project includes a GitHub Actions workflow (`.github/workflows/release.yml`) that automatically builds jlink bundles for all platforms when you create a release tag:

```bash
git tag v1.0.0
git push origin v1.0.0
```

This creates releases with:
- `fake-data-all.jar` - Universal fat JAR (requires Java 21+)
- `fake-data-linux.zip` - Linux x64 jlink bundle (includes JRE)
- `fake-data-macos-arm64.zip` - macOS Apple Silicon jlink bundle (includes JRE)
- `fake-data-macos-x64.zip` - macOS Intel jlink bundle (includes JRE)
- `fake-data-windows.zip` - Windows jlink bundle (includes JRE)

## License

MIT License
