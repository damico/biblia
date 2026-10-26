-- Word alignments between editions, the SBLGNT Greek New Testament (MACULA Greek) and the
-- Almeida 1911 New Testament aligned to it (Clear Bible Alignments, commit c99bd0a).
-- Passages, tokens and alignments are loaded by scripts/import-sblgnt.sh and scripts/import-jfa11.sh.

-- One row per linked pair: a token of an original language edition and a token of a translation.
CREATE TABLE token_alignments (
    source_record_id      INT NOT NULL REFERENCES source_records (id),
    original_token_id     BIGINT NOT NULL REFERENCES tokens (id) ON DELETE CASCADE,
    translation_token_id  BIGINT NOT NULL REFERENCES tokens (id) ON DELETE CASCADE,
    PRIMARY KEY (source_record_id, original_token_id, translation_token_id)
);

CREATE INDEX token_alignments_translation_idx ON token_alignments (translation_token_id);

INSERT INTO source_records (name, maintainer, url, version_ref, obtained_on, license, attribution,
                            coverage, modifications, known_limitations)
VALUES (
    'MACULA Greek Linguistic Datasets (SBLGNT)',
    'Biblica, Inc. (Clear Bible); texto: Society of Biblical Literature e Logos Bible Software',
    'https://github.com/Clear-Bible/macula-greek/tree/main/SBLGNT',
    '8423afe47b9e8f24b7772e808af45c7159a6fe7e',
    DATE '2026-10-25',
    'Creative Commons Atribuição 4.0 Internacional (CC BY 4.0)',
    'MACULA Greek Linguistic Datasets, available at https://github.com/Clear-Bible/macula-greek/ '
        || '(© 2022-2024 Biblica, Inc., CC BY 4.0). Texto grego: SBL Greek New Testament, Copyright 2010 '
        || 'Society of Biblical Literature e Logos Bible Software (CC BY 4.0), https://sblgnt.com/. '
        || 'Glosas em inglês: Berean Interlinear Bible (domínio público).',
    'Novo Testamento (27 livros); arquivo SBLGNT/tsv/macula-greek-SBLGNT.tsv.',
    'Texto de cada versículo reconstruído a partir das palavras e da pontuação seguinte (coluna after); '
        || 'lema, classe gramatical, morfologia, Strong e glosa importados por palavra.',
    'Análises migradas da edição N1904 do MACULA onde as palavras coincidem; leituras exclusivas do SBLGNT '
        || 'vêm das árvores sintáticas da Clear Bible. Domínios Louw-Nida não importados.'
);

INSERT INTO source_records (name, maintainer, url, version_ref, obtained_on, license, attribution,
                            coverage, modifications, known_limitations)
VALUES (
    'Clear Bible Alignments: Almeida 1911 (JFA11) e alinhamento SBLGNT-JFA11',
    'BiblioNexus; Biblica, Inc. (Clear Bible)',
    'https://github.com/Clear-Bible/Alignments',
    'c99bd0ae6946775f932517656308ca19fc706921',
    DATE '2026-10-25',
    'Texto da Almeida 1911 em domínio público; tokenização e alinhamento CC BY 4.0',
    'A Bíblia Sagrada, Edição Revista e Corrigida (Almeida 1911), domínio público '
        || '(https://www.gutenberg.org/ebooks/62383). Tokenização e alinhamento SBLGNT-JFA11: '
        || 'Copyright © 2024 BiblioNexus, CC BY 4.0, via Clear Bible Alignments '
        || '(https://github.com/Clear-Bible/Alignments).',
    'Novo Testamento; arquivos data/por/targets/JFA11/nt_JFA11.tsv e '
        || 'data/por/alignments/JFA11/SBLGNT-JFA11-transfer.json.',
    'Texto de cada versículo reconstruído a partir dos tokens (coluna skip_space_after); pontuação mantida '
        || 'como tokens próprios.',
    'Alinhamento transferido automaticamente do espanhol (RVR09), sem revisão manual em português: pode conter '
        || 'erros. Apenas pares de uma palavra para uma palavra; muitas palavras portuguesas ficam sem par. '
        || 'Ortografia original de 1911.'
);

INSERT INTO textual_works (code, name, description)
VALUES ('sblgnt', 'SBL Greek New Testament', 'Novo Testamento grego da Society of Biblical Literature (2010).'),
       ('almeida1911', 'Almeida 1911', 'Tradução de João Ferreira de Almeida, edição revista e corrigida de 1911.');

INSERT INTO editions (code, textual_work_id, language_code, source_record_id, name, version, editorial_criteria)
SELECT 'sblgnt', w.id, 'grc', s.id, 'SBL Greek New Testament (MACULA Greek)', 'MACULA 8423afe',
       'Texto crítico de 2010; lemas e morfologia segundo o MACULA Greek.'
  FROM textual_works w
  JOIN source_records s ON s.url = 'https://github.com/Clear-Bible/macula-greek/tree/main/SBLGNT'
                       AND s.version_ref = '8423afe47b9e8f24b7772e808af45c7159a6fe7e'
 WHERE w.code = 'sblgnt';

INSERT INTO editions (code, textual_work_id, language_code, source_record_id, name, version, editorial_criteria)
SELECT 'jfa11', w.id, 'por', s.id, 'Almeida 1911', 'Clear Bible Alignments c99bd0a',
       'Novo Testamento com palavras alinhadas ao SBLGNT.'
  FROM textual_works w
  JOIN source_records s ON s.url = 'https://github.com/Clear-Bible/Alignments'
                       AND s.version_ref = 'c99bd0ae6946775f932517656308ca19fc706921'
 WHERE w.code = 'almeida1911';
