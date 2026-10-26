#!/usr/bin/env bash
# Imports a MACULA text with morphology into an empty edition:
#   n1904  - Nestle 1904 Greek New Testament (macula-greek, Nestle1904/tsv)
#   sblgnt - SBL Greek New Testament (macula-greek, SBLGNT/tsv)
#   wlc    - Westminster Leningrad Codex, Hebrew and Aramaic (macula-hebrew, WLC/tsv, stored in Git LFS)
# Runs on the test VM from ~/biblia, after db/migrate.sh. --replace reloads an edition already
# imported (its tokens are deleted, with the alignments that point to them).
#
# Usage: scripts/import-macula.sh n1904|sblgnt|wlc [--replace]

set -euo pipefail

GREEK_COMMIT="8423afe47b9e8f24b7772e808af45c7159a6fe7e"
HEBREW_COMMIT="47db250bd55d0d8577f2a94fba114ef16c35b23c"
EDITION="${1:-n1904}"
REPLACE="false"
if [[ "${2:-}" == "--replace" ]]; then
    REPLACE="true"
fi
case "$EDITION" in
    n1904)
        COMMIT="$GREEK_COMMIT"
        URL="https://raw.githubusercontent.com/Clear-Bible/macula-greek/$COMMIT/Nestle1904/tsv/macula-greek-Nestle1904.tsv" ;;
    sblgnt)
        COMMIT="$GREEK_COMMIT"
        URL="https://raw.githubusercontent.com/Clear-Bible/macula-greek/$COMMIT/SBLGNT/tsv/macula-greek-SBLGNT.tsv" ;;
    wlc)
        COMMIT="$HEBREW_COMMIT"
        URL="https://media.githubusercontent.com/media/Clear-Bible/macula-hebrew/$COMMIT/WLC/tsv/macula-hebrew.tsv" ;;
    *) echo "Unknown edition: $EDITION (expected n1904, sblgnt or wlc)" >&2; exit 1 ;;
esac
SOURCE_PATH="${URL##*/}"

BASE_DIR="$(cd "$(dirname "$0")/.." && pwd)"
SOURCES_DIR="$BASE_DIR/sources"
SOURCE_FILE="$SOURCES_DIR/macula-$COMMIT-$SOURCE_PATH"

if [[ ! -f "$SOURCE_FILE" ]]; then
    mkdir -p "$SOURCES_DIR"
    curl -sfL -o "$SOURCE_FILE.part" "$URL"
    mv "$SOURCE_FILE.part" "$SOURCE_FILE"
fi

java -jar "${JAR:-$BASE_DIR/biblia.jar}" \
    --spring.main.web-application-type=none \
    --import.edition="$EDITION" \
    --import.macula.file="$SOURCE_FILE" \
    --import.replace="$REPLACE"
