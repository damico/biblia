#!/usr/bin/env bash
# Imports the Old Testament of the Almeida 1911 (edition "jfa11") from the Project Gutenberg transcription
# of the 1911 Lisbon printing (eBook #62383). Runs on the test VM from ~/biblia, after db/migrate.sh;
# the edition must have no Old Testament yet (its New Testament comes from scripts/import-jfa11.sh).
# Run scripts/import-versification.sh afterwards.
#
# Usage: scripts/import-jfa11-ot.sh

set -euo pipefail

# Gutenberg does not version its files: the checksum pins the copy that was validated
# (pg62383.txt, "Most recently updated: October 18, 2024").
SHA256="bf9c6053bfb441d117a2a128d4edec9fbe935f415bc8f7bf4e1f3f9121790215"
URL="https://www.gutenberg.org/cache/epub/62383/pg62383.txt"
BASE_DIR="$(cd "$(dirname "$0")/.." && pwd)"
SOURCES_DIR="$BASE_DIR/sources"
SOURCE_FILE="$SOURCES_DIR/pg62383-2024-10-18.txt"

if [[ ! -f "$SOURCE_FILE" ]]; then
    mkdir -p "$SOURCES_DIR"
    curl -sfL -o "$SOURCE_FILE.part" "$URL"
    mv "$SOURCE_FILE.part" "$SOURCE_FILE"
fi
echo "$SHA256  $SOURCE_FILE" | sha256sum -c --quiet

java -jar "${JAR:-$BASE_DIR/biblia.jar}" \
    --spring.main.web-application-type=none \
    --import.edition=jfa11 \
    --import.gutenberg.file="$SOURCE_FILE"
