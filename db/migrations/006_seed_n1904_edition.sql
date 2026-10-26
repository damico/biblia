-- Nestle 1904 Greek New Testament with MACULA Greek morphology, commit 8423afe (2026-07-07).
-- Passages, tokens and analyses are loaded by scripts/import-macula.sh, not by this script.

INSERT INTO source_records (name, maintainer, url, version_ref, obtained_on, license, attribution,
                            coverage, modifications, known_limitations)
VALUES (
    'MACULA Greek Linguistic Datasets (Nestle 1904)',
    'Biblica, Inc. (Clear Bible)',
    'https://github.com/Clear-Bible/macula-greek',
    '8423afe47b9e8f24b7772e808af45c7159a6fe7e',
    DATE '2026-10-25',
    'Creative Commons Atribuição 4.0 Internacional (CC BY 4.0) para o MACULA; texto grego de 1904 e glosas '
        || 'em domínio público; morfologia de base (biblicalhumanities.org) CC0',
    'MACULA Greek Linguistic Datasets, available at https://github.com/Clear-Bible/macula-greek/ '
        || '(© 2022-2024 Biblica, Inc., CC BY 4.0). Texto grego: Nestle 1904, editado por Eberhard Nestle, '
        || 'British and Foreign Bible Society; transcrição de Diego Santos, morfologia de Ulrik Sandborg-Petersen, '
        || 'marcação de Jonathan Robie. Glosas em inglês: Berean Interlinear Bible (domínio público).',
    'Novo Testamento (27 livros); arquivo Nestle1904/tsv/macula-greek-Nestle1904.tsv.',
    'Texto de cada versículo reconstruído a partir das palavras e da pontuação seguinte (coluna after); '
        || 'lema, classe gramatical, morfologia, Strong e glosa importados por palavra.',
    'Domínios semânticos Louw-Nida (colunas domain e ln, dados UBS MARBLE usados com permissão) não importados. '
        || 'Glosas apenas em inglês.'
);

INSERT INTO textual_works (code, name, description)
VALUES ('nestle1904', 'Nestle 1904', 'Novo Testamento grego editado por Eberhard Nestle (1904).');

INSERT INTO editions (code, textual_work_id, language_code, source_record_id, name, version, editorial_criteria)
SELECT 'n1904', w.id, 'grc', s.id, 'Nestle 1904 (MACULA Greek)', 'MACULA 8423afe',
       'Texto crítico de 1904; lemas e morfologia segundo o MACULA Greek.'
  FROM textual_works w
  JOIN source_records s ON s.url = 'https://github.com/Clear-Bible/macula-greek'
                       AND s.version_ref = '8423afe47b9e8f24b7772e808af45c7159a6fe7e'
 WHERE w.code = 'nestle1904';
