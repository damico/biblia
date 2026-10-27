#!/usr/bin/env bash
# Generates the audio of every chapter of a Portuguese edition (reading aloud) on the test VM, one process
# per GPU. Needs the application running (text comes from its API), ffmpeg and an NVIDIA driver. The Python
# environment (Kokoro, PyTorch with CUDA, about 5 GB) is created in ~/biblia/tts/venv on the first run.
# Chapters already generated are skipped; output goes to ~/biblia/audio/<edition>/ (or AUDIO_OUT, to build a
# new version beside the one being served and swap the folders afterwards).
#
# Usage: scripts/generate-audio.sh <edition> [book]     e.g. scripts/generate-audio.sh blivre-n4

set -euo pipefail

EDITION="$1"
BOOK="${2:-}"
BASE_DIR="$(cd "$(dirname "$0")/.." && pwd)"
VENV="$BASE_DIR/tts/venv"
OUT="${AUDIO_OUT:-$BASE_DIR/audio}"

if [[ ! -x "$VENV/bin/python" ]]; then
    python3 -m venv "$VENV"
    "$VENV/bin/pip" install -q -r "$BASE_DIR/scripts/audio-requirements.txt"
fi

GPUS=$(nvidia-smi --list-gpus | wc -l)
pids=()
for ((i = 0; i < GPUS; i++)); do
    "$VENV/bin/python" "$BASE_DIR/scripts/generate-audio.py" --edition "$EDITION" --out "$OUT" \
        --device "cuda:$i" --shard "$i/$GPUS" ${BOOK:+--book "$BOOK"} &
    pids+=($!)
done
status=0
for pid in "${pids[@]}"; do
    wait "$pid" || status=1
done
exit $status
