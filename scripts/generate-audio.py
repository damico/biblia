"""Reads every chapter of a Portuguese edition aloud with Kokoro 82M (voice pf_dora) and writes one audio file
per chapter with the start and end of each verse. Run by scripts/generate-audio.sh on the test VM.

Text comes from the running application (API), phonemes from the Kokoro pipeline for Brazilian Portuguese
(misaki EspeakG2P, espeak-ng "pt-br"). Output, under AUDIO_DIR/<edition>/<book>/:
  <chapter>.m4a   AAC, mono, 24 kHz
  <chapter>.json  {"duration": s, "verses": [{"verse": n, "start": s, "end": s}, ...]}
Chapters that already have both files are skipped, so an interrupted run can be resumed.
"""
import argparse
import json
import os
import re
import subprocess
import sys
import time
import urllib.error
import urllib.request

import numpy as np
import torch
from kokoro import KModel, KPipeline

REPO = 'hexgrad/Kokoro-82M'
VOICE = 'pf_dora'
RATE = 24000
MAX_PHONEMES = 500           # the model reads at most 510 tokens
TITLE_PAUSE = 0.8            # seconds of silence after the chapter title
VERSE_PAUSE = 0.35           # seconds of silence between verses
PIECE_PAUSE = 0.15           # between the pieces of a verse too long for the model
API_ATTEMPTS = 30          # 10 s apart
GLIDE_BEFORE_WORD = re.compile(r'w(?= )')
CLAUSE_END = re.compile(r'(?<=[.;:!?,])\s+')


def get_json(base, path):
    """Waits for the application while it restarts (a deploy during a run)."""
    for attempt in range(API_ATTEMPTS):
        try:
            with urllib.request.urlopen(base + path) as response:
                return json.load(response)
        except urllib.error.URLError as error:
            if attempt == API_ATTEMPTS - 1 or getattr(error, 'code', 500) < 500:
                raise
            time.sleep(10)


def pieces(pipeline, text):
    """Phonemes of the text in pieces the model can read, cut at punctuation when the verse is too long."""
    phonemes, _ = pipeline.g2p(text)
    if len(phonemes) <= MAX_PHONEMES:
        return [phonemes]
    result = []
    current = ''
    for clause in CLAUSE_END.split(text):
        candidate = (current + ' ' + clause).strip()
        if current and len(pipeline.g2p(candidate)[0]) > MAX_PHONEMES:
            result.append(pipeline.g2p(current)[0])
            current = clause
        else:
            current = candidate
    if current:
        result.append(pipeline.g2p(current)[0])
    too_long = [p for p in result if len(p) > MAX_PHONEMES]
    if too_long:
        raise ValueError('Clause too long for the model (%d phonemes): %s' % (len(too_long[0]), text))
    return result


def title_phonemes(pipeline, chapter):
    """ "João. Capítulo 1." with the last vowel of "capítulo" kept apart: espeak-ng joins it to a number that
    starts with a vowel ("capitulw um"), which sounds like "capítulo onze"."""
    phonemes, _ = pipeline.g2p('%s. Capítulo %d.' % (chapter['book']['name'], chapter['chapter']))
    return GLIDE_BEFORE_WORD.sub('\u028A', phonemes)


def speak(model, voice, phoneme_pieces):
    audio = []
    for i, phonemes in enumerate(phoneme_pieces):
        if i > 0:
            audio.append(np.zeros(int(PIECE_PAUSE * RATE), dtype=np.float32))
        audio.append(KPipeline.infer(model, phonemes, voice).audio.cpu().numpy())
    return np.concatenate(audio)


def encode(samples, path):
    tmp = path + '.part'
    subprocess.run(['ffmpeg', '-v', 'error', '-y', '-f', 'f32le', '-ar', str(RATE), '-ac', '1', '-i', '-',
                    '-c:a', 'aac', '-b:a', '40k', '-movflags', '+faststart', '-f', 'mp4', tmp],
                   input=samples.astype(np.float32).tobytes(), check=True)
    os.replace(tmp, path)


def chapter_audio(model, pipeline, voice, chapter):
    parts = [speak(model, voice, [title_phonemes(pipeline, chapter)]),
             np.zeros(int(TITLE_PAUSE * RATE), dtype=np.float32)]
    position = sum(len(p) for p in parts)
    verses = []
    for passage in chapter['passages']:
        samples = speak(model, voice, pieces(pipeline, passage['content']))
        verses.append({'verse': passage['verse'], 'start': round(position / RATE, 3),
                       'end': round((position + len(samples)) / RATE, 3)})
        parts += [samples, np.zeros(int(VERSE_PAUSE * RATE), dtype=np.float32)]
        position += len(samples) + int(VERSE_PAUSE * RATE)
    return np.concatenate(parts), verses


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--edition', required=True)
    parser.add_argument('--api', default='http://localhost:8089/biblia')
    parser.add_argument('--out', required=True)
    parser.add_argument('--device', default='cuda:0')
    parser.add_argument('--shard', default='0/1', help='i/n: only books whose index modulo n is i')
    parser.add_argument('--book', help='only this book code')
    args = parser.parse_args()
    shard, shards = (int(x) for x in args.shard.split('/'))

    model = KModel(repo_id=REPO).to(args.device).eval()
    pipeline = KPipeline(lang_code='p', repo_id=REPO, model=False)
    voice = pipeline.load_voice(VOICE)

    books = get_json(args.api, '/api/editions/%s/books' % args.edition)
    for index, book in enumerate(books):
        if index % shards != shard or (args.book and book['code'] != args.book):
            continue
        folder = os.path.join(args.out, args.edition, book['code'])
        os.makedirs(folder, exist_ok=True)
        for number in range(1, book['chapterCount'] + 1):
            audio_path = os.path.join(folder, '%d.m4a' % number)
            json_path = os.path.join(folder, '%d.json' % number)
            if os.path.exists(audio_path) and os.path.exists(json_path):
                continue
            chapter = get_json(args.api, '/api/editions/%s/books/%s/chapters/%d'
                               % (args.edition, book['code'], number))
            with torch.inference_mode():
                samples, verses = chapter_audio(model, pipeline, voice, chapter)
            encode(samples, audio_path)
            with open(json_path + '.part', 'w') as f:
                json.dump({'duration': round(len(samples) / RATE, 3), 'verses': verses}, f)
            os.replace(json_path + '.part', json_path)
            print('%s %d: %.0f s' % (book['code'], number, len(samples) / RATE), flush=True)


if __name__ == '__main__':
    sys.exit(main())
