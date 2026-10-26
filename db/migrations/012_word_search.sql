-- Word search: accent-insensitive search in translations and search in the English glosses.
-- The materialized views are refreshed at the end of every import (MaintenanceRepository).

CREATE EXTENSION IF NOT EXISTS unaccent;

-- Distinct glosses of each lexeme in each edition, with how many analysed segments carry them.
CREATE MATERIALIZED VIEW lexeme_glosses AS
SELECT t.edition_id, a.lexeme_id, a.gloss, count(*) AS occurrences
  FROM token_analyses a
  JOIN tokens t ON t.id = a.token_id
 WHERE a.gloss IS NOT NULL
 GROUP BY t.edition_id, a.lexeme_id, a.gloss;

CREATE INDEX lexeme_glosses_lexeme_idx ON lexeme_glosses (lexeme_id);
CREATE INDEX lexeme_glosses_edition_idx ON lexeme_glosses (edition_id);

-- Number of distinct verses where each lexeme occurs in each edition.
CREATE MATERIALIZED VIEW lexeme_verse_counts AS
SELECT t.edition_id, a.lexeme_id, count(DISTINCT t.passage_id) AS verses
  FROM token_analyses a
  JOIN tokens t ON t.id = a.token_id
 GROUP BY t.edition_id, a.lexeme_id;

CREATE UNIQUE INDEX lexeme_verse_counts_idx ON lexeme_verse_counts (edition_id, lexeme_id);
