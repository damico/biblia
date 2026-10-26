#!/usr/bin/env bash
# Computes the standard verse references (STEPBible TVTMS) of every edition already imported, plus the
# manual corrections in scripts/data/versification-corrections.tsv. Runs on the test VM from ~/biblia;
# run it again after importing or reloading any edition.
#
# Usage: scripts/import-versification.sh

set -euo pipefail

COMMIT="be4191f0082aeedddb0eff0c02c966292b2bd641"
FILE_NAME="TVTMS - Translators Versification Traditions with Methodology for Standardisation for Eng+Heb+Lat+Grk+Others - STEPBible.org CC BY.txt"
BASE_DIR="$(cd "$(dirname "$0")/.." && pwd)"
SOURCES_DIR="$BASE_DIR/sources"
SOURCE_FILE="$SOURCES_DIR/tvtms-$COMMIT.txt"

if [[ ! -f "$SOURCE_FILE" ]]; then
    mkdir -p "$SOURCES_DIR"
    ENCODED_NAME="$(python3 -c 'import sys, urllib.parse; print(urllib.parse.quote(sys.argv[1]))' "$FILE_NAME")"
    curl -sfL -o "$SOURCE_FILE.part" \
        "https://raw.githubusercontent.com/STEPBible/STEPBible-Data/$COMMIT/Versification/$ENCODED_NAME"
    mv "$SOURCE_FILE.part" "$SOURCE_FILE"
fi

java -jar "${JAR:-$BASE_DIR/biblia.jar}" \
    --spring.main.web-application-type=none \
    --import.versification.file="$SOURCE_FILE" \
    --import.versification.corrections="$BASE_DIR/scripts/data/versification-corrections.tsv" \
    --import.versification.version="$COMMIT"
