-- Verse correspondence through a standard reference system (STEPBible TVTMS, English/NRSV standard).
-- Each passage of each edition gets one or more standard references; two editions correspond where
-- their passages share a standard reference. Filled by scripts/import-versification.sh.
-- Only verses are mapped: no word level correspondence is derived from this table.

-- relation:
--   same        the edition numbers this text like the standard (no rule needed, or a "keep" rule)
--   renumbered  the same text under another number (e.g. Hebrew Ps 3:2 = standard Ps 3:1)
--   title       the text is (or contains) a psalm title, standard verse 0
--   partial     the passage holds only part of the standard verse, or more than one standard verse
--               (standard_part names the part when the rule gives one: a, b, ...)
-- source_record_id is null when no TVTMS rule was used: verses numbered like the standard, psalm headings
-- and corrections registered by hand (rule_action starts with "manual").
CREATE TABLE passage_standard_refs (
    passage_id          BIGINT NOT NULL REFERENCES passages (id) ON DELETE CASCADE,
    seq                 INT NOT NULL,
    standard_book_id    INT NOT NULL REFERENCES books (id),
    standard_chapter    INT NOT NULL CHECK (standard_chapter > 0),
    standard_verse      INT NOT NULL CHECK (standard_verse >= 0),
    standard_part       TEXT NOT NULL DEFAULT '',
    relation            TEXT NOT NULL CHECK (relation IN ('same', 'renumbered', 'title', 'partial')),
    rule_action         TEXT,
    source_record_id    INT REFERENCES source_records (id),
    PRIMARY KEY (passage_id, seq)
);

CREATE INDEX passage_standard_refs_standard_idx
    ON passage_standard_refs (standard_book_id, standard_chapter, standard_verse);

INSERT INTO source_records (name, maintainer, url, version_ref, obtained_on, license, attribution,
                            coverage, modifications, known_limitations)
VALUES (
    'TVTMS - Translators Versification Traditions with Methodology for Standardisation',
    'STEPBible.org (Tyndale House, Cambridge)',
    'https://github.com/STEPBible/STEPBible-Data',
    'be4191f0082aeedddb0eff0c02c966292b2bd641',
    DATE '2026-10-25',
    'Creative Commons Atribuição 4.0 Internacional (CC BY 4.0)',
    'Versificação: TVTMS (Translators Versification Traditions with Methodology for Standardisation), '
        || 'STEPBible.org, Tyndale House, Cambridge, CC BY 4.0 (https://github.com/STEPBible/STEPBible-Data).',
    'Regras de correspondência de versículos entre as tradições inglesa, hebraica, latina e grega; '
        || 'arquivo Versification/TVTMS ... .txt, seção Expanded.',
    'Regras avaliadas sobre os dados de cada edição (existência de versículos, último versículo do capítulo, '
        || 'contagem de palavras, título antes do v. 1). Subversículos da fonte não são usados: um versículo '
        || 'que corresponde a partes de outros fica com relação parcial.',
    'Defeitos de arquivo de uma edição específica não são previstos pelo TVTMS e entram como correções manuais.'
);
