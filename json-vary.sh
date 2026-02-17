#!/bin/bash
#
# json-vary.sh - Generate N copies of a JSON file with randomised replacements.
#
# Usage:
#   ./json-vary.sh <source.json> <count> <value1> [value2] [value3] ...
#
# Each value is a literal string found in the source JSON. The script
# auto-detects its type from the value's format and generates a random
# replacement of the same shape. All occurrences are replaced (string
# matching, not JSON-path matching).
#
# Pattern detection:
#   - All digits (10+ chars)           -> random digits, same length
#   - XXXX-XXXX-XXXX (alphanumeric)    -> random alphanumeric groups (submission ref)
#   - UUID (8-4-4-4-12 hex)            -> random UUID
#   - Anything else                    -> random business name from built-in list
#
# Output files are named after the generated submission-reference value
# (the XXXX-XXXX-XXXX pattern). If no such pattern exists, files are
# named with a sequential number.
#
# Produces a summary CSV (vary-manifest.csv) mapping filenames to values.
#
# Dependencies: bash, sed, od, tr - no Python, no uuidgen.

set -euo pipefail

# --- Built-in business name list ---
BUSINESS_NAMES=(
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

# --- Random generators (portable: /dev/urandom + od) ---

# Generate random digits of given length
gen_digits() {
  local len=$1
  local result=""
  while [ ${#result} -lt "$len" ]; do
    # Read 4 bytes as unsigned int, take digits
    local num
    num=$(od -An -tu4 -N4 /dev/urandom | tr -d ' ')
    result="${result}${num}"
  done
  echo "${result:0:$len}"
}

# Generate random alphanumeric string of given length (uppercase + digits)
gen_alnum() {
  local len=$1
  local result
  result=$(od -An -tx1 -N"$((len * 2))" /dev/urandom | tr -d ' \n' | tr 'abcdef' 'ABCDEF' | sed 's/[^A-Z0-9]//g')
  # Map hex chars to broader alphanumeric range
  result=$(LC_ALL=C tr -dc 'A-Z0-9' < /dev/urandom | head -c "$len" 2>/dev/null || true)
  if [ ${#result} -lt "$len" ]; then
    # Fallback: build char by char
    result=""
    local charset="ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
    for ((i=0; i<len; i++)); do
      local idx
      idx=$(od -An -tu1 -N1 /dev/urandom | tr -d ' ')
      idx=$((idx % 36))
      result="${result}${charset:$idx:1}"
    done
  fi
  echo "$result"
}

# Generate UUID (8-4-4-4-12 lowercase hex)
gen_uuid() {
  local hex
  hex=$(od -An -tx1 -N16 /dev/urandom | tr -d ' \n')
  printf '%s-%s-%s-%s-%s\n' \
    "${hex:0:8}" "${hex:8:4}" "${hex:12:4}" "${hex:16:4}" "${hex:20:12}"
}

# Generate submission reference matching pattern like T9Q0-IIIB-PP52
# Detects group sizes from the original value
gen_submission_ref() {
  local original=$1
  local result=""
  local IFS='-'
  read -ra groups <<< "$original"
  for ((g=0; g<${#groups[@]}; g++)); do
    local group_len=${#groups[$g]}
    local part
    part=$(gen_alnum "$group_len")
    if [ "$g" -gt 0 ]; then
      result="${result}-"
    fi
    result="${result}${part}"
  done
  echo "$result"
}

# Pick a random business name
gen_business_name() {
  local idx
  idx=$(od -An -tu2 -N2 /dev/urandom | tr -d ' ')
  idx=$((idx % ${#BUSINESS_NAMES[@]}))
  echo "${BUSINESS_NAMES[$idx]}"
}

# --- Pattern detection ---
# Returns: digits, submission_ref, uuid, text
detect_pattern() {
  local val=$1

  # UUID: 8-4-4-4-12 hex pattern
  if echo "$val" | grep -qE '^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$'; then
    echo "uuid"
    return
  fi

  # Submission reference: groups of alphanumeric separated by hyphens (2-6 chars per group)
  if echo "$val" | grep -qE '^[A-Za-z0-9]{2,6}(-[A-Za-z0-9]{2,6})+$'; then
    echo "submission_ref"
    return
  fi

  # All digits, 4+ chars
  if echo "$val" | grep -qE '^[0-9]{4,}$'; then
    echo "digits"
    return
  fi

  # Fallback: free text
  echo "text"
}

# Generate a replacement value for a given original value
generate_replacement() {
  local original=$1
  local pattern
  pattern=$(detect_pattern "$original")

  case "$pattern" in
    uuid)
      gen_uuid
      ;;
    submission_ref)
      gen_submission_ref "$original"
      ;;
    digits)
      gen_digits ${#original}
      ;;
    text)
      gen_business_name
      ;;
  esac
}

# --- Escape string for use in sed ---
sed_escape() {
  # Escape sed special chars: \ / & . * [ ] ^ $
  printf '%s' "$1" | sed -e 's/[\/&.*[\^$]/\\&/g'
}

# --- Main ---

show_help() {
  echo "Usage: $0 <source.json> <count> <value1> [value2] [value3] ..."
  echo ""
  echo "Generates <count> copies of source.json with each <value> replaced"
  echo "by a random value matching its detected pattern."
  echo ""
  echo "  Pattern detection (automatic):"
  echo "    All digits (4+ chars)        -> random digits, same length"
  echo "    XXXX-XXXX-XXXX (alphanum)    -> random alphanumeric groups"
  echo "    UUID hex (8-4-4-4-12)        -> random UUID"
  echo "    Anything else                -> random business name"
  echo ""
  echo "  Output files named after the generated submission-reference."
  echo "  Summary written to vary-manifest.csv in the output directory."
  echo ""
  echo "Example:"
  echo "  $0 creative-relief.json 10 \\"
  echo "    '9237766545' \\"
  echo "    'T9Q0-IIIB-PP52' \\"
  echo "    'a8d91e74-2285-4582-9d7c-fe6b400da347' \\"
  echo "    'SUA tec04'"
}

if [[ "${1:-}" == "-h" || "${1:-}" == "--help" ]]; then
  show_help
  exit 0
fi

if [ $# -lt 3 ]; then
  show_help
  exit 1
fi

SOURCE_FILE=$1
COUNT=$2
shift 2
ORIGINAL_VALUES=("$@")

if [ ! -f "$SOURCE_FILE" ]; then
  echo "Error: Source file not found: $SOURCE_FILE" >&2
  exit 1
fi

if ! [[ "$COUNT" =~ ^[0-9]+$ ]] || [ "$COUNT" -lt 1 ]; then
  echo "Error: Count must be a positive integer, got: $COUNT" >&2
  exit 1
fi

# Detect patterns and show what we found
echo "Source: $SOURCE_FILE"
echo "Copies: $COUNT"
echo ""
echo "Values to replace:"

SUBMISSION_REF_IDX=-1
for ((i=0; i<${#ORIGINAL_VALUES[@]}; i++)); do
  local_pattern=$(detect_pattern "${ORIGINAL_VALUES[$i]}")
  echo "  [$((i+1))] \"${ORIGINAL_VALUES[$i]}\" -> $local_pattern"
  if [ "$local_pattern" = "submission_ref" ] && [ "$SUBMISSION_REF_IDX" -eq -1 ]; then
    SUBMISSION_REF_IDX=$i
  fi
done
echo ""

# Sort values by length (longest first) to avoid substring collision
# Build index array sorted by string length descending
SORTED_INDICES=()
for ((i=0; i<${#ORIGINAL_VALUES[@]}; i++)); do
  SORTED_INDICES+=("$i")
done

# Bubble sort by length (descending) - fine for small arrays
for ((i=0; i<${#SORTED_INDICES[@]}; i++)); do
  for ((j=i+1; j<${#SORTED_INDICES[@]}; j++)); do
    len_i=${#ORIGINAL_VALUES[${SORTED_INDICES[$i]}]}
    len_j=${#ORIGINAL_VALUES[${SORTED_INDICES[$j]}]}
    if [ "$len_j" -gt "$len_i" ]; then
      tmp=${SORTED_INDICES[$i]}
      SORTED_INDICES[$i]=${SORTED_INDICES[$j]}
      SORTED_INDICES[$j]=$tmp
    fi
  done
done

# Create output directory
OUTPUT_DIR="./json-vary-output"
mkdir -p "$OUTPUT_DIR"

# Write manifest header
MANIFEST="$OUTPUT_DIR/vary-manifest.csv"
{
  printf "filename"
  for ((i=0; i<${#ORIGINAL_VALUES[@]}; i++)); do
    printf ",original_%d" "$((i+1))"
  done
  printf "\n"
} > "$MANIFEST"

# Verify all values exist in source
SOURCE_CONTENT=$(cat "$SOURCE_FILE")
for ((i=0; i<${#ORIGINAL_VALUES[@]}; i++)); do
  if ! echo "$SOURCE_CONTENT" | grep -qF "${ORIGINAL_VALUES[$i]}"; then
    echo "Warning: Value not found in source: \"${ORIGINAL_VALUES[$i]}\"" >&2
  fi
done

# Generate copies
echo "Generating $COUNT files..."

for ((n=1; n<=COUNT; n++)); do
  # Generate replacements for each value
  REPLACEMENTS=()
  for ((i=0; i<${#ORIGINAL_VALUES[@]}; i++)); do
    REPLACEMENTS+=("$(generate_replacement "${ORIGINAL_VALUES[$i]}")")
  done

  # Determine output filename
  if [ "$SUBMISSION_REF_IDX" -ge 0 ]; then
    OUT_NAME="${REPLACEMENTS[$SUBMISSION_REF_IDX]}.json"
  else
    OUT_NAME=$(printf "varied_%03d.json" "$n")
  fi

  # Build sed command: replace longest strings first
  CONTENT="$SOURCE_CONTENT"
  for idx in "${SORTED_INDICES[@]}"; do
    local_orig=$(sed_escape "${ORIGINAL_VALUES[$idx]}")
    local_repl=$(sed_escape "${REPLACEMENTS[$idx]}")
    CONTENT=$(echo "$CONTENT" | sed "s/${local_orig}/${local_repl}/g")
  done

  # Write output file
  echo "$CONTENT" > "$OUTPUT_DIR/$OUT_NAME"

  # Append to manifest
  {
    printf "%s" "$OUT_NAME"
    for ((i=0; i<${#REPLACEMENTS[@]}; i++)); do
      printf ",%s" "${REPLACEMENTS[$i]}"
    done
    printf "\n"
  } >> "$MANIFEST"

  echo "  [$n/$COUNT] $OUT_NAME"
done

echo ""
echo "Output: $OUTPUT_DIR/"
echo "Manifest: $MANIFEST"
echo "Done."
