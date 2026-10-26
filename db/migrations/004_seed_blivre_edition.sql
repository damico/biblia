-- Biblia Livre (BLIVRE), n4 variant (New Testament from Nestle 1904), version 2025.1.0.
-- Verses are loaded by the importer (see CLAUDE.md), not by this script.

INSERT INTO source_records (name, maintainer, url, version_ref, obtained_on, license, attribution,
                            coverage, modifications, known_limitations)
VALUES (
    'Bíblia Livre (BLIVRE)',
    'Diego Santos, Mario Sérgio e Marco Teles',
    'https://github.com/blivre/BibliaLivre',
    'a315a15e9f4d01883b62206fe441d57762f126b3',
    DATE '2026-10-25',
    'Creative Commons Atribuição 3.0 Brasil (CC BY 3.0 BR)',
    'Todas as Escrituras em português citadas são da Bíblia Livre (BLIVRE), Copyright © Diego Santos, '
        || 'Mario Sérgio e Marco Teles, http://sites.google.com/site/biblialivre/ - versão 2025.1.0, janeiro de 2025. '
        || 'Licença Creative Commons Atribuição 3.0 Brasil (http://creativecommons.org/licenses/by/3.0/br/). '
        || 'Reprodução permitida desde que devidamente mencionados fonte e autores.',
    '66 livros; arquivos textos/f4/n4 (Antigo Testamento comum às variantes, Novo Testamento a partir do Nestle 1904).',
    'Quebras de linha e espaços repetidos normalizados para um espaço; marcadores \added, \fn, \key, \ref e '
        || '\psalm-title convertidos em anotações; marcação malformada na origem corrigida pelo importador, '
        || 'com cada correção registrada no log da importação.',
    'Tradução em andamento, com revisão parcial do Novo Testamento (ver status-revisao.md na origem).'
);

INSERT INTO textual_works (code, name, description)
VALUES ('blivre', 'Bíblia Livre', 'Tradução das Escrituras para o português sob licença livre.');

INSERT INTO editions (code, textual_work_id, language_code, source_record_id, name, version, editorial_criteria)
SELECT 'blivre-n4', w.id, 'por', s.id, 'Bíblia Livre (n4)', '2025.1.0',
       'Novo Testamento traduzido a partir do Nestle 1904 (texto crítico).'
  FROM textual_works w
  JOIN source_records s ON s.url = 'https://github.com/blivre/BibliaLivre'
                       AND s.version_ref = 'a315a15e9f4d01883b62206fe441d57762f126b3'
 WHERE w.code = 'blivre';
