-- Segment level analyses (Hebrew and Aramaic prefixes, stems and suffixes) and the
-- Westminster Leningrad Codex with MACULA Hebrew morphology, commit 47db250 (2026-04-24).
-- Passages, tokens and analyses are loaded by scripts/import-macula.sh wlc.

-- segment_text is the written part of the token analysed by the row (null when the analysis
-- covers the whole token; empty for an article implied by the vowels of a preposition).
ALTER TABLE token_analyses
    ADD COLUMN segment_text     TEXT,
    ADD COLUMN language_code    TEXT REFERENCES languages (code),
    ADD COLUMN transliteration  TEXT,
    ADD COLUMN stem             TEXT,
    ADD COLUMN verb_form        TEXT,
    ADD COLUMN state            TEXT;

UPDATE token_analyses a
   SET language_code = l.language_code
  FROM lexemes l
 WHERE l.id = a.lexeme_id;

ALTER TABLE token_analyses ALTER COLUMN language_code SET NOT NULL;

INSERT INTO source_records (name, maintainer, url, version_ref, obtained_on, license, attribution,
                            coverage, modifications, known_limitations)
VALUES (
    'MACULA Hebrew Linguistic Datasets (WLC)',
    'Biblica, Inc. (Clear Bible)',
    'https://github.com/Clear-Bible/macula-hebrew',
    '47db250bd55d0d8577f2a94fba114ef16c35b23c',
    DATE '2026-10-25',
    'Creative Commons Atribuição 4.0 Internacional (CC BY 4.0); texto hebraico WLC de cópia livre',
    'MACULA Hebrew Linguistic Datasets, available at https://github.com/Clear-Bible/macula-hebrew/ '
        || '(© 2022-2024 Biblica, Inc., CC BY 4.0). Texto: Westminster Leningrad Codex (tanach.us), cópia sem '
        || 'restrições. Morfologia: OpenScriptures Hebrew Bible (https://hb.openscriptures.org), CC BY 4.0. '
        || 'Glosas em inglês: Cherith Glosses for the Hebrew Old Testament, Andi Wu, © 2022 Cherith Analytics, '
        || 'CC BY 4.0.',
    'Antigo Testamento hebraico e aramaico (39 livros); arquivo WLC/tsv/macula-hebrew.tsv.',
    'Cada palavra gráfica é um token; prefixos, raízes e sufixos são segmentos com análise própria. '
        || 'Texto do versículo reconstruído a partir dos segmentos e da coluna after (maqaf, sof pasuq, '
        || 'setumah e petuhah preservados). Forma normalizada para busca sem acentos de cantilação.',
    'Numeração de versículos hebraica (difere das traduções em Salmos com título, Joel, Malaquias e outros). '
        || 'Onde há Ketiv e Qere, o MACULA registra o Qere. Domínios do Semantic Dictionary of Biblical Hebrew '
        || '(UBS, usados com permissão) não importados.'
);

INSERT INTO textual_works (code, name, description)
VALUES ('wlc', 'Códice de Leningrado', 'Texto Massorético da Bíblia Hebraica segundo o Westminster Leningrad Codex.');

INSERT INTO editions (code, textual_work_id, language_code, source_record_id, name, version, editorial_criteria)
SELECT 'wlc', w.id, 'hbo', s.id, 'Códice de Leningrado (MACULA Hebrew)', 'MACULA 47db250',
       'Texto Massorético com trechos em aramaico (indicados por segmento); morfologia do OSHB.'
  FROM textual_works w
  JOIN source_records s ON s.url = 'https://github.com/Clear-Bible/macula-hebrew'
                       AND s.version_ref = '47db250bd55d0d8577f2a94fba114ef16c35b23c'
 WHERE w.code = 'wlc';
