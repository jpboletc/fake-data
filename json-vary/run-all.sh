#!/bin/bash
#
# run-all.sh - Generate varied JSON submissions and matching renamed attachments.
#
# For each JSON template in the templates directory, generates N varied
# copies with specified literal strings replaced by pattern-matched random
# values. Then copies and renames attachments for each generated submission
# reference, producing a master manifest CSV.
#
# Pattern detection (automatic from value format):
#   - All digits (4+ chars)           -> random digits, same length
#   - XXXX-XXXX-XXXX (alphanumeric)   -> random alphanumeric groups
#   - UUID (8-4-4-4-12 hex)           -> random UUID
#   - Anything else                   -> random business name
#
# Idempotent: cleans the output directory before each run.
#
# Usage:
#   ./run-all.sh [options] <value1> [value2] ...
#
# Options:
#   -n, --variations N        Number of varied copies per template (default: 7)
#   -m, --max-attachments N   Max attachments per submission (0 = all, default: 0)
#   -t, --templates <dir>     Templates directory (default: ./templates)
#   -a, --attachments <dir>   Source attachments directory (default: ./attachments)
#   -o, --output <dir>        Output directory (default: ./output)
#   --manifests N             Number of manifest files (default: 1)
#   -H, --start-hour HH      Starting hour 0-23 for manifests (default: current hour)
#   -h, --help                Show this help
#
# Examples:
#   ./run-all.sh '9237766545' 'T9Q0-IIIB-PP52' 'a8d91e74-2285-4582-9d7c-fe6b400da347' 'SUA tec04'
#   ./run-all.sh -n 10 -m 3 -o ./results '9237766545' 'T9Q0-IIIB-PP52'

set -euo pipefail

# ============================================================
# Defaults
# ============================================================

TEMPLATES_DIR="./templates"
ATTACHMENTS_DIR="./attachments"
VARIATIONS=7
MAX_ATTACHMENTS=0
OUTPUT="./output"
MANIFESTS=1
START_HOUR=-1

# ============================================================
# Business name list
# ============================================================

# Company name word lists (100 each) - combined to generate thousands of unique names
NAME_ADJECTIVES=(
  Blue       Golden     Silver     Pacific    Northern   Southern   Atlantic   Alpine     Coastal    Sterling
  Amber      Crimson    Emerald    Sapphire   Cedar      Maple      Iron       Copper     Granite    Crystal
  Coral      Arctic     Solar      Lunar      Stellar    Cascade    Pinnacle   Meridian   Horizon    Frontier
  Heritage   Legacy     Pioneer    Eagle      Falcon     Phoenix    Sentinel   Titan      Aurora     Nova
  Quantum    Vertex     Prism      Fusion     Catalyst   Beacon     Compass    Anchor     Keystone   Landmark
  Swift      Agile      Dynamic    Elite      Noble      Bright     Vital      Allied     United     Metro
  Continental Premier   Prime      Royal      Grand      Summit     Crest      Aspen      Birch      Elm
  Oak        Pine       Sage       Flint      Onyx       Cobalt     Scarlet    Indigo     Azure      Polaris
  Orion      Triton     Vega       Zenith     Apex       Evergreen  Redwood    Sequoia    Sierra     Boreal
  Tidal      Radiant    Integral   Resolute   Sovereign  Astra      Trident    Central    Civic      Global
)

NAME_NOUNS=(
  Ridge      Harbor     Creek      Valley     Stone      Field      Grove      Bridge     Gate       Tower
  Park       Bay        Cove       Hill       Glen       Brook      Forge      River      Cliff      Point
  Landing    Crossing   Dale       Haven      Shore      Rock       Peak       Canyon     Mesa       Trail
  Vista      Hollow     Ledge      Basin      Arch       Pier       Coast      Port       Inlet      Cape
  Terrace    Knoll      Spur       Bend       Crown      Shield     Spire      Spring     Lake       Marsh
  Bluff      Prairie    Glade      Dune       Range      Channel    Strand     Reef       Oasis      Plateau
  Heath      Moor       Dell       Trace      Edge       Bond       Line       Core       Link       Nexus
  Pulse      Spark      Wave       Tide       Axis       Orbit      Lens       Venture    Capital    Croft
  Wells      Gardens    Meadow     Commons    Quarter    Circuit    Loop       Reach      Pass       Strait
  Pointe     Pines      Sands      Springs    Heights    Narrows    Rapids     Falls      Harbour    Wharf
)

NAME_SUFFIXES=(
  Holdings   Group      Partners   Corp       Industries Solutions  Systems    Technologies Dynamics  Ventures
  Capital    Consulting Enterprises Associates Services  Analytics  Advisors   Global     International Networks
  Digital    Labs       Works      Co         Ltd        Inc        Media      Energy     Logistics  Supply
  Direct     Alliance   Collective Foundation Institute  Agency     Trust      Fund       Exchange   Trade
  Commerce   Properties Development Design    Creative   Studio     Research   Resources  Materials  Products
  Financial  Health     Scientific Engineering Manufacturing Biotech Software  Intelligence Security Communications
  Aerospace  Marine     Automotive Pharma     Medical    Electronics Robotics  Strategies Advisory   Management
  Freight    Transport  Aviation   Defence    Power      Mining     Agriculture Construction Realty   Telecom
  Wireless   Cloud      Data       Hardware   Textiles   Metals     Minerals   Nutrition  Foods      Chemicals
  Optics     Devices    Components Instruments Brands    Concepts   Express    Connect    Source     Platform
)

# ============================================================
# Random generators (portable: /dev/urandom + od)
# ============================================================

gen_digits() {
  local len=$1
  local result=""
  while [ ${#result} -lt "$len" ]; do
    local num
    num=$(od -An -tu4 -N4 /dev/urandom | tr -d ' ')
    result="${result}${num}"
  done
  echo "${result:0:$len}"
}

gen_alnum() {
  local len=$1
  local charset="ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
  local result=""
  for ((i=0; i<len; i++)); do
    local idx
    idx=$(od -An -tu1 -N1 /dev/urandom | tr -d ' ')
    idx=$((idx % 36))
    result="${result}${charset:$idx:1}"
  done
  echo "$result"
}

gen_uuid() {
  local hex
  hex=$(od -An -tx1 -N16 /dev/urandom | tr -d ' \n')
  printf '%s-%s-%s-%s-%s\n' \
    "${hex:0:8}" "${hex:8:4}" "${hex:12:4}" "${hex:16:4}" "${hex:20:12}"
}

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

gen_business_name() {
  local adj_idx noun_idx suffix_idx pattern
  adj_idx=$(( $(od -An -tu2 -N2 /dev/urandom | tr -d ' ') % ${#NAME_ADJECTIVES[@]} ))
  noun_idx=$(( $(od -An -tu2 -N2 /dev/urandom | tr -d ' ') % ${#NAME_NOUNS[@]} ))
  suffix_idx=$(( $(od -An -tu2 -N2 /dev/urandom | tr -d ' ') % ${#NAME_SUFFIXES[@]} ))
  pattern=$(( $(od -An -tu1 -N1 /dev/urandom | tr -d ' ') % 5 ))
  if [ "$pattern" -le 2 ]; then
    echo "${NAME_ADJECTIVES[$adj_idx]} ${NAME_NOUNS[$noun_idx]} ${NAME_SUFFIXES[$suffix_idx]}"
  elif [ "$pattern" -eq 3 ]; then
    echo "${NAME_NOUNS[$noun_idx]} ${NAME_SUFFIXES[$suffix_idx]}"
  else
    echo "${NAME_ADJECTIVES[$adj_idx]} ${NAME_SUFFIXES[$suffix_idx]}"
  fi
}

gen_manifest_id() {
  local charset="abcdefghijklmnopqrstuvwxyz0123456789"
  local result=""
  for ((i=0; i<16; i++)); do
    local idx
    idx=$(od -An -tu1 -N1 /dev/urandom | tr -d ' ')
    idx=$((idx % 36))
    result="${result}${charset:$idx:1}"
  done
  echo "$result"
}

# ============================================================
# Pattern detection
# ============================================================

detect_pattern() {
  local val=$1

  # UUID: 8-4-4-4-12 hex
  if echo "$val" | grep -qE '^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$'; then
    echo "uuid"
    return
  fi

  # Submission reference: groups of alphanumeric separated by hyphens
  if echo "$val" | grep -qE '^[A-Za-z0-9]{2,6}(-[A-Za-z0-9]{2,6})+$'; then
    echo "submission_ref"
    return
  fi

  # All digits, 4+ chars
  if echo "$val" | grep -qE '^[0-9]{4,}$'; then
    echo "digits"
    return
  fi

  echo "text"
}

generate_replacement() {
  local original=$1
  local pattern
  pattern=$(detect_pattern "$original")

  case "$pattern" in
    uuid)           gen_uuid ;;
    submission_ref) gen_submission_ref "$original" ;;
    digits)         gen_digits ${#original} ;;
    text)           gen_business_name ;;
  esac
}

# ============================================================
# Help
# ============================================================

show_help() {
  cat <<'HELP'
Usage: run-all.sh [options] <value1> [value2] ...

Generate varied JSON submissions and matching renamed attachments.

Options:
  -n, --variations N        Number of varied copies per template (default: 7)
  -m, --max-attachments N   Max attachments per submission (0 = all, default: 0)
  -t, --templates <dir>     Templates directory (default: ./templates)
  -a, --attachments <dir>   Source attachments directory (default: ./attachments)
  -o, --output <dir>        Output directory (default: ./output)
  --manifests N             Number of manifest files (default: 1)
  -H, --start-hour HH      Starting hour 0-23 for manifests (default: current hour)
  -h, --help                Show this help

Pattern detection (automatic from value format):
  All digits (4+ chars)        -> random digits, same length
  XXXX-XXXX-XXXX (alphanum)    -> random alphanumeric groups
  UUID hex (8-4-4-4-12)        -> random UUID
  Anything else                -> random business name

Examples:
  ./run-all.sh '9237766545' 'T9Q0-IIIB-PP52' 'a8d91e74-2285-4582-9d7c-fe6b400da347' 'SUA tec04'
  ./run-all.sh -n 10 -m 3 -o ./results '9237766545' 'T9Q0-IIIB-PP52'
  ./run-all.sh --manifests 3 -H 15 '9237766545' 'T9Q0-IIIB-PP52' 'a8d91e74...' 'SUA tec04'
HELP
}

# ============================================================
# Parse options
# ============================================================

VALUES=()
while [[ $# -gt 0 ]]; do
  case "$1" in
    -h|--help)
      show_help
      exit 0
      ;;
    -n|--variations)
      VARIATIONS="$2"
      shift 2
      ;;
    -m|--max-attachments)
      MAX_ATTACHMENTS="$2"
      shift 2
      ;;
    -t|--templates)
      TEMPLATES_DIR="$2"
      shift 2
      ;;
    -a|--attachments)
      ATTACHMENTS_DIR="$2"
      shift 2
      ;;
    -o|--output)
      OUTPUT="$2"
      shift 2
      ;;
    --manifests)
      MANIFESTS="$2"
      shift 2
      ;;
    -H|--start-hour)
      START_HOUR="$2"
      shift 2
      ;;
    -*)
      echo "Unknown option: $1" >&2
      show_help
      exit 1
      ;;
    *)
      VALUES+=("$1")
      shift
      ;;
  esac
done

# ============================================================
# Validation
# ============================================================

if [ ${#VALUES[@]} -eq 0 ]; then
  echo "Error: At least one value to replace is required." >&2
  echo ""
  show_help
  exit 1
fi

if [ ! -d "$TEMPLATES_DIR" ]; then
  echo "Error: Templates directory not found: $TEMPLATES_DIR" >&2
  exit 1
fi

# Collect template files sorted by name
TEMPLATES=()
while IFS= read -r -d '' f; do
  TEMPLATES+=("$f")
done < <(find "$TEMPLATES_DIR" -maxdepth 1 -name '*.json' -print0 | sort -z)

if [ ${#TEMPLATES[@]} -eq 0 ]; then
  echo "Error: No JSON files found in: $TEMPLATES_DIR" >&2
  exit 1
fi

if ! [[ "$VARIATIONS" =~ ^[0-9]+$ ]] || [ "$VARIATIONS" -lt 1 ]; then
  echo "Error: Variations must be a positive integer, got: $VARIATIONS" >&2
  exit 1
fi

# Safety check: never delete source attachments dir
resolved_output=$(cd "$(dirname "$OUTPUT")" 2>/dev/null && echo "$(pwd)/$(basename "$OUTPUT")" || echo "$OUTPUT")
resolved_attach=$(cd "$ATTACHMENTS_DIR" 2>/dev/null && pwd || echo "$ATTACHMENTS_DIR")
if [ "$resolved_output" = "$resolved_attach" ]; then
  echo "Error: Output directory cannot be the same as attachments directory" >&2
  exit 1
fi

if ! [[ "$MANIFESTS" =~ ^[0-9]+$ ]] || [ "$MANIFESTS" -lt 1 ]; then
  echo "Error: Manifests must be a positive integer, got: $MANIFESTS" >&2
  exit 1
fi

if [ "$START_HOUR" -eq -1 ]; then
  START_HOUR=$(date +"%H")
  # Strip leading zero for arithmetic
  START_HOUR=$((10#$START_HOUR))
elif [ "$START_HOUR" -lt 0 ] || [ "$START_HOUR" -gt 23 ]; then
  echo "Error: Start hour must be 0-23, got: $START_HOUR" >&2
  exit 1
fi

# ============================================================
# Prepare: detect submission ref index, sort by length
# ============================================================

SUBMISSION_REF_IDX=-1
for ((i=0; i<${#VALUES[@]}; i++)); do
  pat=$(detect_pattern "${VALUES[$i]}")
  if [ "$pat" = "submission_ref" ] && [ "$SUBMISSION_REF_IDX" -eq -1 ]; then
    SUBMISSION_REF_IDX=$i
  fi
done

# Sort indices by value length (longest first) to avoid substring collision
SORTED_INDICES=()
for ((i=0; i<${#VALUES[@]}; i++)); do
  SORTED_INDICES+=("$i")
done

for ((i=0; i<${#SORTED_INDICES[@]}; i++)); do
  for ((j=i+1; j<${#SORTED_INDICES[@]}; j++)); do
    len_i=${#VALUES[${SORTED_INDICES[$i]}]}
    len_j=${#VALUES[${SORTED_INDICES[$j]}]}
    if [ "$len_j" -gt "$len_i" ]; then
      tmp=${SORTED_INDICES[$i]}
      SORTED_INDICES[$i]=${SORTED_INDICES[$j]}
      SORTED_INDICES[$j]=$tmp
    fi
  done
done

# ============================================================
# Summary
# ============================================================

echo "Templates:       $TEMPLATES_DIR (${#TEMPLATES[@]} files)"
echo "Attachments:     $ATTACHMENTS_DIR"
echo "Variations:      $VARIATIONS per template"
if [ "$MAX_ATTACHMENTS" -gt 0 ]; then
  echo "Max attachments: $MAX_ATTACHMENTS per submission"
else
  echo "Max attachments: all available"
fi
echo "Output:          $OUTPUT"
if [ "$MANIFESTS" -gt 1 ]; then
  echo "Manifests:       $MANIFESTS (starting at hour $(printf '%02d' $START_HOUR))"
fi
echo ""
echo "Values to replace:"
for ((i=0; i<${#VALUES[@]}; i++)); do
  pat=$(detect_pattern "${VALUES[$i]}")
  printf '  [%d] "%s" -> %s\n' "$((i+1))" "${VALUES[$i]}" "$pat"
done
echo ""

# ============================================================
# Clean output directory for idempotent runs
# ============================================================

if [ -d "$OUTPUT" ]; then
  echo "Cleaning previous output: $OUTPUT"
  rm -rf "$OUTPUT"
fi

# ============================================================
# Phase 1: Generate varied JSON files
# ============================================================

echo "--- Phase 1: Generating varied JSON files ---"
echo ""

template_num=1
for template in "${TEMPLATES[@]}"; do
  template_basename=$(basename "$template" .json)
  out_dir="$OUTPUT/$template_basename"
  mkdir -p "$out_dir"

  source_content=$(cat "$template")

  # Verify all values exist in template
  for ((i=0; i<${#VALUES[@]}; i++)); do
    if ! echo "$source_content" | grep -qF "${VALUES[$i]}"; then
      echo "Warning: Value not found in $(basename "$template"): \"${VALUES[$i]}\"" >&2
    fi
  done

  echo "=== [$template_num/${#TEMPLATES[@]}] $(basename "$template") -> $template_basename ==="

  for ((n=1; n<=VARIATIONS; n++)); do
    # Generate replacements
    REPLACEMENTS=()
    for ((i=0; i<${#VALUES[@]}; i++)); do
      REPLACEMENTS+=("$(generate_replacement "${VALUES[$i]}")")
    done

    # Determine output filename
    if [ "$SUBMISSION_REF_IDX" -ge 0 ]; then
      out_name="${REPLACEMENTS[$SUBMISSION_REF_IDX]}.json"
    else
      out_name=$(printf "varied_%03d.json" "$n")
    fi

    # Replace longest strings first using awk for reliable substitution
    content="$source_content"
    for idx in "${SORTED_INDICES[@]}"; do
      original="${VALUES[$idx]}"
      replacement="${REPLACEMENTS[$idx]}"
      content=$(awk -v orig="$original" -v repl="$replacement" '{
        while (i = index($0, orig)) {
          $0 = substr($0, 1, i-1) repl substr($0, i+length(orig))
        }
        print
      }' <<< "$content")
    done

    # Write output file (no trailing newline)
    printf '%s' "$content" > "$out_dir/$out_name"

    echo "  [$n/$VARIATIONS] $out_name"
  done

  echo ""
  template_num=$((template_num + 1))
done

# ============================================================
# Phase 2: Copy and rename attachments
# ============================================================

echo "--- Phase 2: Copying attachments ---"
echo ""

attachments_out_dir="$OUTPUT/attachments"
mkdir -p "$attachments_out_dir"

# Create manifest file(s) with empty first line, no header
# Use epoch-seconds arithmetic for portable midnight rollover
if date -r 0 +%s >/dev/null 2>&1; then
  # macOS/BSD: date -j -f to parse, date -r to format from epoch
  base_epoch=$(date -j -f "%Y%m%d%H%M%S" "$(date +%Y%m%d)$(printf '%02d' $START_HOUR)0000" "+%s")
  date_from_epoch() { date -r "$1" +"$2"; }
else
  # GNU/Linux: date -d to parse, date -d @epoch to format
  base_epoch=$(date -d "$(date +%Y-%m-%d) $(printf '%02d' $START_HOUR):00:00" "+%s")
  date_from_epoch() { date -d "@$1" +"$2"; }
fi

MANIFEST_PATHS=()
for ((mi=0; mi<MANIFESTS; mi++)); do
  epoch=$((base_epoch + mi * 3600))
  stamp=$(date_from_epoch "$epoch" "%d%m%y%H")
  path="$attachments_out_dir/manifest${stamp}.csv"
  echo "" > "$path"
  MANIFEST_PATHS+=("$path")
done

total_copied=0
for template in "${TEMPLATES[@]}"; do
  template_basename=$(basename "$template" .json)
  json_dir="$OUTPUT/$template_basename"
  src_attach_dir="$ATTACHMENTS_DIR/$template_basename"

  if [ ! -d "$src_attach_dir" ]; then
    echo "Warning: No attachments directory for '$template_basename' at $src_attach_dir - skipping" >&2
    continue
  fi

  # Get source attachment files (exclude CSV files)
  SOURCE_ATTACHMENTS=()
  while IFS= read -r -d '' f; do
    SOURCE_ATTACHMENTS+=("$f")
  done < <(find "$src_attach_dir" -maxdepth 1 -type f ! -name '*.csv' -print0 | sort -z)

  if [ ${#SOURCE_ATTACHMENTS[@]} -eq 0 ]; then
    echo "Warning: No attachment files found in $src_attach_dir" >&2
    continue
  fi

  # Apply max attachments limit
  if [ "$MAX_ATTACHMENTS" -gt 0 ] && [ ${#SOURCE_ATTACHMENTS[@]} -gt "$MAX_ATTACHMENTS" ]; then
    SOURCE_ATTACHMENTS=("${SOURCE_ATTACHMENTS[@]:0:$MAX_ATTACHMENTS}")
  fi

  # Detect original prefix from first attachment filename
  first_file=$(basename "${SOURCE_ATTACHMENTS[0]}")
  if [[ "$first_file" =~ ^([^_]+)_ ]]; then
    original_prefix="${BASH_REMATCH[1]}"
  else
    echo "Warning: Cannot detect prefix from '$first_file' - skipping $template_basename" >&2
    continue
  fi

  # Get generated JSON files for this template
  JSON_FILES=()
  while IFS= read -r -d '' f; do
    JSON_FILES+=("$f")
  done < <(find "$json_dir" -maxdepth 1 -name '*.json' -print0 2>/dev/null | sort -z)

  if [ ${#JSON_FILES[@]} -eq 0 ]; then
    echo "Warning: No JSON files found in $json_dir" >&2
    continue
  fi

  echo "=== ${template_basename}: ${#JSON_FILES[@]} submissions x ${#SOURCE_ATTACHMENTS[@]} attachments (prefix: ${original_prefix}) ==="

  for json_file in "${JSON_FILES[@]}"; do
    # Submission reference is the filename without .json
    submission_ref=$(basename "$json_file" .json)
    # Remove hyphens for filename prefix
    file_prefix="${submission_ref//-/}"

    for attachment in "${SOURCE_ATTACHMENTS[@]}"; do
      attach_name=$(basename "$attachment")
      new_name="${attach_name/$original_prefix/$file_prefix}"
      cp "$attachment" "$attachments_out_dir/$new_name"

      mail_item_id=$(gen_manifest_id)
      attached_id=$(gen_manifest_id)
      manifest_idx=$(( $(od -An -tu2 -N2 /dev/urandom | tr -d ' ') % MANIFESTS ))
      echo "${mail_item_id},${attached_id},${new_name}" >> "${MANIFEST_PATHS[$manifest_idx]}"

      total_copied=$((total_copied + 1))
    done
    echo "  $submission_ref -> ${#SOURCE_ATTACHMENTS[@]} files"
  done

  echo ""
done

# ============================================================
# Summary
# ============================================================

echo "Attachments: $attachments_out_dir/"
if [ ${#MANIFEST_PATHS[@]} -eq 1 ]; then
  echo "Manifest:    ${MANIFEST_PATHS[0]}"
else
  echo "Manifests:   ${#MANIFEST_PATHS[@]} files"
  for mp in "${MANIFEST_PATHS[@]}"; do
    echo "             $(basename "$mp")"
  done
fi
echo "Total files: $total_copied attachments"
echo ""
echo "All done. ${#TEMPLATES[@]} templates x $VARIATIONS variations = $(( ${#TEMPLATES[@]} * VARIATIONS )) submissions."
