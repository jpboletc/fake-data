#!/bin/bash
#
# post-attachments.sh - POST attachment files in batched multipart requests.
#
# Collects all files from the attachments directory and sends them in
# batched multipart/form-data POST requests. Manifest CSV files are always
# included in the final batch so that they arrive after the files they
# reference.
#
# Usage:
#   ./post-attachments.sh [options] -u <uri> -t <auth-token> -d <attachments-dir>
#
# Options:
#   -u, --uri <url>          Endpoint URL to POST to (required)
#   -t, --auth-token <str>   Full Basic auth string (required)
#   -d, --attachments-dir <dir>  Directory containing attachment files (required)
#   -b, --batch-size N       Max files per POST request (default: 40)
#   -k, --skip-cert-check    Skip SSL certificate validation
#   -h, --help               Show this help
#
# Example:
#   ./post-attachments.sh \
#     -u 'https://localhost:8080/api/cavr/attachments/s3files' \
#     -t 'Basic On53YF0/fGA4SS9JQCUlIX51MEUq' \
#     -d './output/attachments'

set -euo pipefail

# ============================================================
# Defaults
# ============================================================

URI=""
AUTH_TOKEN=""
ATTACHMENTS_DIR=""
BATCH_SIZE=40
SKIP_CERT_CHECK=false

# ============================================================
# Content-type mapping
# ============================================================

get_content_type() {
  local ext="${1##*.}"
  ext=$(echo "$ext" | tr '[:upper:]' '[:lower:]')
  case "$ext" in
    pdf)  echo "application/pdf" ;;
    csv)  echo "text/csv" ;;
    json) echo "application/json" ;;
    jpeg|jpg) echo "image/jpeg" ;;
    png)  echo "image/png" ;;
    xlsx) echo "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" ;;
    xls)  echo "application/vnd.ms-excel" ;;
    docx) echo "application/vnd.openxmlformats-officedocument.wordprocessingml.document" ;;
    doc)  echo "application/msword" ;;
    pptx) echo "application/vnd.openxmlformats-officedocument.presentationml.presentation" ;;
    ods)  echo "application/vnd.oasis.opendocument.spreadsheet" ;;
    odt)  echo "application/vnd.oasis.opendocument.text" ;;
    odp)  echo "application/vnd.oasis.opendocument.presentation" ;;
    *)    echo "application/octet-stream" ;;
  esac
}

# ============================================================
# Help
# ============================================================

show_help() {
  cat <<'HELP'
Usage: post-attachments.sh [options] -u <uri> -t <auth-token> -d <attachments-dir>

POST attachment files in batched multipart requests.

Options:
  -u, --uri <url>              Endpoint URL to POST to (required)
  -t, --auth-token <str>       Full Basic auth string (required)
  -d, --attachments-dir <dir>  Directory containing attachment files (required)
  -b, --batch-size N           Max files per POST request (default: 40)
  -k, --skip-cert-check        Skip SSL certificate validation
  -h, --help                   Show this help

Manifest CSV files (manifest*.csv) are always sent in the final batch.

Example:
  ./post-attachments.sh \
    -u 'https://localhost:8080/api/cavr/attachments/s3files' \
    -t 'Basic On53YF0/fGA4SS9JQCUlIX51MEUq' \
    -d './output/attachments'
HELP
}

# ============================================================
# Parse options
# ============================================================

while [[ $# -gt 0 ]]; do
  case "$1" in
    -h|--help)
      show_help
      exit 0
      ;;
    -u|--uri)
      URI="$2"
      shift 2
      ;;
    -t|--auth-token)
      AUTH_TOKEN="$2"
      shift 2
      ;;
    -d|--attachments-dir)
      ATTACHMENTS_DIR="$2"
      shift 2
      ;;
    -b|--batch-size)
      BATCH_SIZE="$2"
      shift 2
      ;;
    -k|--skip-cert-check)
      SKIP_CERT_CHECK=true
      shift
      ;;
    *)
      echo "Unknown option: $1" >&2
      show_help
      exit 1
      ;;
  esac
done

# ============================================================
# Validation
# ============================================================

if [ -z "$URI" ]; then
  echo "Error: --uri is required" >&2
  show_help
  exit 1
fi

if [ -z "$AUTH_TOKEN" ]; then
  echo "Error: --auth-token is required" >&2
  show_help
  exit 1
fi

if [ -z "$ATTACHMENTS_DIR" ]; then
  echo "Error: --attachments-dir is required" >&2
  show_help
  exit 1
fi

if [ ! -d "$ATTACHMENTS_DIR" ]; then
  echo "Error: Attachments directory not found: $ATTACHMENTS_DIR" >&2
  exit 1
fi

if ! [[ "$BATCH_SIZE" =~ ^[0-9]+$ ]] || [ "$BATCH_SIZE" -lt 1 ]; then
  echo "Error: Batch size must be a positive integer, got: $BATCH_SIZE" >&2
  exit 1
fi

# Build curl SSL flag
CURL_INSECURE=""
if [ "$SKIP_CERT_CHECK" = true ]; then
  CURL_INSECURE="-k"
fi

# ============================================================
# Collect files, separating manifests from attachments
# ============================================================

MANIFEST_FILES=()
ATTACHMENT_FILES=()

while IFS= read -r -d '' f; do
  fname=$(basename "$f")
  if [[ "$fname" =~ ^manifest.*\.csv$ ]]; then
    MANIFEST_FILES+=("$f")
  else
    ATTACHMENT_FILES+=("$f")
  fi
done < <(find "$ATTACHMENTS_DIR" -maxdepth 1 -type f -print0 | sort -z)

total_files=$(( ${#ATTACHMENT_FILES[@]} + ${#MANIFEST_FILES[@]} ))

if [ "$total_files" -eq 0 ]; then
  echo "Error: No files found in: $ATTACHMENTS_DIR" >&2
  exit 1
fi

# Calculate total size
total_size=0
for f in "${ATTACHMENT_FILES[@]}" "${MANIFEST_FILES[@]}"; do
  fsize=$(stat -c%s "$f" 2>/dev/null || stat -f%z "$f" 2>/dev/null || echo 0)
  total_size=$((total_size + fsize))
done
total_size_mb=$(awk "BEGIN {printf \"%.2f\", $total_size / 1048576}")

echo "Endpoint:    $URI"
echo "Attachments: $ATTACHMENTS_DIR"
echo "Files:       $total_files ($total_size_mb MB)"
echo "  Attachments: ${#ATTACHMENT_FILES[@]}"
echo "  Manifests:   ${#MANIFEST_FILES[@]} (sent in final batch)"
echo "Batch size:  $BATCH_SIZE"
echo ""

# ============================================================
# Build batches
# ============================================================

# Split attachment files into batches
declare -a BATCH_START=()
declare -a BATCH_END=()

for ((i=0; i<${#ATTACHMENT_FILES[@]}; i+=BATCH_SIZE)); do
  end=$((i + BATCH_SIZE))
  if [ "$end" -gt "${#ATTACHMENT_FILES[@]}" ]; then
    end=${#ATTACHMENT_FILES[@]}
  fi
  BATCH_START+=("$i")
  BATCH_END+=("$end")
done

total_batches=${#BATCH_START[@]}
if [ "$total_batches" -eq 0 ]; then
  total_batches=1
  BATCH_START=(0)
  BATCH_END=(0)
fi

# Manifests go into the final batch
MANIFESTS_IN_LAST=true

echo "Batches:     $total_batches"
echo ""

# ============================================================
# Send batches
# ============================================================

uploaded_count=0

for ((b=0; b<total_batches; b++)); do
  batch_num=$((b + 1))

  # Collect files for this batch
  batch_files=()
  start=${BATCH_START[$b]}
  end=${BATCH_END[$b]}
  for ((i=start; i<end; i++)); do
    batch_files+=("${ATTACHMENT_FILES[$i]}")
  done

  # Append manifests to the last batch
  if [ "$batch_num" -eq "$total_batches" ]; then
    for mf in "${MANIFEST_FILES[@]}"; do
      batch_files+=("$mf")
    done
  fi

  echo "--- Batch $batch_num / $total_batches (${#batch_files[@]} files) ---"

  # Build curl -F arguments for this batch
  curl_args=()
  for f in "${batch_files[@]}"; do
    fname=$(basename "$f")
    ct=$(get_content_type "$fname")
    fsize=$(stat -c%s "$f" 2>/dev/null || stat -f%z "$f" 2>/dev/null || echo 0)
    fsize_kb=$(awk "BEGIN {printf \"%.1f\", $fsize / 1024}")
    echo "  Added: $fname ($ct, $fsize_kb KB)"
    curl_args+=(-F "files=@${f};type=${ct}")
  done

  echo "  Sending POST..."

  http_code=$(curl -s -o /tmp/post-attachments-response.txt -w "%{http_code}" \
    $CURL_INSECURE \
    -X POST "$URI" \
    -H "Authorization: $AUTH_TOKEN" \
    -H "Accept: application/hal+json" \
    "${curl_args[@]}" \
    2>&1) || true

  if [[ "$http_code" =~ ^2[0-9][0-9]$ ]]; then
    response=$(head -c 500 /tmp/post-attachments-response.txt 2>/dev/null || true)
    echo "  Response: $http_code OK"
    [ -n "$response" ] && echo "  $response"
    echo ""
    uploaded_count=$((uploaded_count + ${#batch_files[@]}))
  else
    response=$(head -c 500 /tmp/post-attachments-response.txt 2>/dev/null || true)
    echo "  Response: $http_code FAILED" >&2
    [ -n "$response" ] && echo "  $response" >&2
    echo ""
    echo "Aborted after $uploaded_count / $total_files files ($batch_num / $total_batches batches)."
    rm -f /tmp/post-attachments-response.txt
    exit 1
  fi
done

rm -f /tmp/post-attachments-response.txt
echo "Done. $uploaded_count files uploaded in $total_batches batch(es)."
