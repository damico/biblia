#!/usr/bin/env bash
# Imports the Almeida 1911 New Testament (edition "jfa11") with its word alignment to the SBLGNT,
# from Clear Bible Alignments. Runs on the test VM from ~/biblia, after db/migrate.sh and
# scripts/import-macula.sh sblgnt. The edition must be empty.
#
# Usage: scripts/import-jfa11.sh

set -euo pipefail

COMMIT="c99bd0ae6946775f932517656308ca19fc706921"
BASE_URL="https://raw.githubusercontent.com/Clear-Bible/Alignments/$COMMIT/data/por"
BASE_DIR="$(cd "$(dirname "$0")/.." && pwd)"
SOURCES_DIR="$BASE_DIR/sources/clear-alignments-$COMMIT"
TOKENS="$SOURCES_DIR/nt_JFA11.tsv"
ALIGNMENT="$SOURCES_DIR/SBLGNT-JFA11-transfer.json"

download() {
    if [[ ! -f "$2" ]]; then
        mkdir -p "$SOURCES_DIR"
        curl -sfL -o "$2.part" "$1"
        mv "$2.part" "$2"
    fi
}

download "$BASE_URL/targets/JFA11/nt_JFA11.tsv" "$TOKENS"
download "$BASE_URL/alignments/JFA11/SBLGNT-JFA11-transfer.json" "$ALIGNMENT"

java -jar "${JAR:-$BASE_DIR/biblia.jar}" \
    --spring.main.web-application-type=none \
    --import.edition=jfa11 \
    --import.alignment.tokens="$TOKENS" \
    --import.alignment.original=sblgnt \
    --import.alignment.file="$ALIGNMENT"
