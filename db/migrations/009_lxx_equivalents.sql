-- Septuagint equivalents of Hebrew and Aramaic segments (MACULA Hebrew columns greek and greekstrong),
-- used to link Old and New Testament occurrences through the Greek word chosen by the Septuagint.
-- Existing WLC rows get the values when the edition is imported again (scripts/import-macula.sh wlc --replace).

ALTER TABLE token_analyses
    ADD COLUMN lxx_greek   TEXT,
    ADD COLUMN lxx_strong  TEXT;

CREATE INDEX token_analyses_lxx_strong_idx ON token_analyses (lxx_strong) WHERE lxx_strong IS NOT NULL;

CREATE INDEX token_analyses_strong_idx ON token_analyses (strong) WHERE strong IS NOT NULL;

UPDATE source_records
   SET modifications = modifications
           || ' Equivalente grego da Septuaginta (forma e Strong) importado por segmento, como registrado no MACULA.'
 WHERE url = 'https://github.com/Clear-Bible/macula-hebrew'
   AND version_ref = '47db250bd55d0d8577f2a94fba114ef16c35b23c';
