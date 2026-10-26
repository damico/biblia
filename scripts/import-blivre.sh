#!/usr/bin/env bash
# Imports the Biblia Livre n4 text (version 2025.1.0) into the edition "blivre-n4".
# Runs on the test VM from ~/biblia, after db/migrate.sh. The edition must be empty.
#
# Usage: scripts/import-blivre.sh

set -euo pipefail

COMMIT="a315a15e9f4d01883b62206fe441d57762f126b3"
BASE_DIR="$(cd "$(dirname "$0")/.." && pwd)"
SOURCES_DIR="$BASE_DIR/sources"
SOURCE_DIR="$SOURCES_DIR/BibliaLivre-$COMMIT"

if [[ ! -d "$SOURCE_DIR" ]]; then
    mkdir -p "$SOURCES_DIR"
    curl -sfL -o "$SOURCES_DIR/blivre-$COMMIT.zip" "https://codeload.github.com/blivre/BibliaLivre/zip/$COMMIT"
    unzip -q -d "$SOURCES_DIR" "$SOURCES_DIR/blivre-$COMMIT.zip"
fi

java -jar "${JAR:-$BASE_DIR/biblia.jar}" \
    --spring.main.web-application-type=none \
    --import.edition=blivre-n4 \
    --import.blivre.dir="$SOURCE_DIR/textos/f4/n4"
