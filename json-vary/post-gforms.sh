#!/bin/bash
#
# post-gforms.sh - POST gForm JSON files one-by-one to an endpoint.
#
# Iterates all *.json files under the given directory (recursively) and
# POSTs each as a multipart/form-data upload matching the browser-captured
# request format.
#
# Usage:
#   ./post-gforms.sh [options] -u <uri> -t <auth-token> -d <json-dir>
#
# Options:
#   -u, --uri <url>          Endpoint URL to POST to (required)
#   -t, --auth-token <str>   Full Basic auth string (required)
#   -d, --json-dir <dir>     Directory containing JSON files (required)
#   -k, --skip-cert-check    Skip SSL certificate validation
#   -h, --help               Show this help
#
# Example:
#   ./post-gforms.sh \
#     -u 'https://localhost:8080/api/source-files/upload-in-file' \
#     -t 'Basic On53YF0/fGA4SS9JQCUlIX51MEUq' \
#     -d './output'

set -euo pipefail

# ============================================================
# Defaults
# ============================================================

URI=""
AUTH_TOKEN=""
JSON_DIR=""
SKIP_CERT_CHECK=false

# ============================================================
# Help
# ============================================================

show_help() {
  cat <<'HELP'
Usage: post-gforms.sh [options] -u <uri> -t <auth-token> -d <json-dir>

POST gForm JSON files one-by-one to an endpoint.

Options:
  -u, --uri <url>          Endpoint URL to POST to (required)
  -t, --auth-token <str>   Full Basic auth string (required)
  -d, --json-dir <dir>     Directory containing JSON files (required)
  -k, --skip-cert-check    Skip SSL certificate validation
  -h, --help               Show this help

Example:
  ./post-gforms.sh \
    -u 'https://localhost:8080/api/source-files/upload-in-file' \
    -t 'Basic On53YF0/fGA4SS9JQCUlIX51MEUq' \
    -d './output'
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
    -d|--json-dir)
      JSON_DIR="$2"
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

if [ -z "$JSON_DIR" ]; then
  echo "Error: --json-dir is required" >&2
  show_help
  exit 1
fi

if [ ! -d "$JSON_DIR" ]; then
  echo "Error: JSON directory not found: $JSON_DIR" >&2
  exit 1
fi

# Collect JSON files recursively
JSON_FILES=()
while IFS= read -r -d '' f; do
  JSON_FILES+=("$f")
done < <(find "$JSON_DIR" -name '*.json' -print0 | sort -z)

if [ ${#JSON_FILES[@]} -eq 0 ]; then
  echo "Error: No JSON files found under: $JSON_DIR" >&2
  exit 1
fi

# Build curl SSL flag
CURL_INSECURE=""
if [ "$SKIP_CERT_CHECK" = true ]; then
  CURL_INSECURE="-k"
fi

echo "Endpoint:    $URI"
echo "JSON dir:    $JSON_DIR"
echo "Files found: ${#JSON_FILES[@]}"
echo ""

successes=0
failures=0
file_num=0

for json_file in "${JSON_FILES[@]}"; do
  file_num=$((file_num + 1))
  filename=$(basename "$json_file")

  # POST as multipart/form-data with field name "file"
  http_code=$(curl -s -o /dev/null -w "%{http_code}" \
    $CURL_INSECURE \
    -X POST "$URI" \
    -H "Authorization: $AUTH_TOKEN" \
    -H "Accept: application/hal+json" \
    -H "Origin: ${URI%/api/*}" \
    -F "file=@${json_file};type=application/json" \
    2>&1) || true

  if [[ "$http_code" =~ ^2[0-9][0-9]$ ]]; then
    echo "  [$file_num/${#JSON_FILES[@]}] $filename -> $http_code OK"
    successes=$((successes + 1))
  else
    echo "  [$file_num/${#JSON_FILES[@]}] $filename -> $http_code FAILED" >&2
    failures=$((failures + 1))
  fi
done

echo ""
echo "Done. $successes succeeded, $failures failed out of ${#JSON_FILES[@]} files."
