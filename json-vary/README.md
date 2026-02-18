# json-vary

A portable bash utility that takes a source JSON file and generates N copies with specified literal strings replaced by random values of the same pattern.

## Requirements

- bash, sed, od, tr (standard on Linux/macOS)
- No Python, no `uuidgen`, no external dependencies

## Usage

```bash
./json-vary.sh <source.json> <count> <value1> [value2] [value3] ...
```

Run `./json-vary.sh -h` for full help.

Each `<value>` is a literal string found in the source JSON. The script auto-detects its type from the format and generates a random replacement of the same shape. All occurrences are replaced using string matching (not JSON-path), so field names don't matter.

### Pattern Detection

| Pattern | Example | Replacement |
|---------|---------|-------------|
| All digits (4+ chars) | `9237766545` | Random digits, same length |
| Alphanumeric groups with hyphens | `T9Q0-IIIB-PP52` | Random alphanumeric groups, same structure |
| UUID (8-4-4-4-12 hex) | `a8d91e74-2285-4582-9d7c-fe6b400da347` | Random UUID |
| Anything else | `SUA tec04` | Random business name from built-in list |

### Example

```bash
./json-vary.sh creative-relief.json 10 \
  '9237766545' \
  'T9Q0-IIIB-PP52' \
  'a8d91e74-2285-4582-9d7c-fe6b400da347' \
  'SUA tec04'
```

This generates 10 copies of `creative-relief.json`, each with all four values replaced by fresh random values. Output files are named after the generated submission reference (e.g., `DYKN-ISSR-MMPZ.json`).

## Output

Files are written to `./output/` alongside a `vary-manifest.csv` mapping each filename to its generated values:

```
output/
  DYKN-ISSR-MMPZ.json
  UTXN-JB5W-WOP9.json
  1NA7-QREQ-3L14.json
  vary-manifest.csv
```

## Notes

- Replacements are applied longest-first to avoid substring collisions
- If no submission-reference pattern is provided, files are named `varied_001.json`, etc.
- The script runs on AWS Linux instances (no macOS-specific tools required)
