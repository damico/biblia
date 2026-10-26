-- Word level data: tokens of an edition, lexemes and per-token linguistic analyses.

-- Ordered occurrence of a word in a passage, as written in the edition.
CREATE TABLE tokens (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    edition_id     INT NOT NULL REFERENCES editions (id),
    passage_id     BIGINT NOT NULL REFERENCES passages (id) ON DELETE CASCADE,
    position       INT NOT NULL CHECK (position > 0),
    source_ref     TEXT NOT NULL,
    surface        TEXT NOT NULL,
    trailing_text  TEXT NOT NULL DEFAULT '',
    normalized     TEXT NOT NULL,
    UNIQUE (passage_id, position),
    UNIQUE (edition_id, source_ref)
);

-- Exact form search ignores case (capitals at sentence start are editorial).
CREATE INDEX tokens_edition_form_idx ON tokens (edition_id, lower(normalized));

-- Dictionary form within one lemma system (the source that assigned it).
CREATE TABLE lexemes (
    id                INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    language_code     TEXT NOT NULL REFERENCES languages (code),
    source_record_id  INT NOT NULL REFERENCES source_records (id),
    lemma             TEXT NOT NULL,
    UNIQUE (language_code, source_record_id, lemma)
);

-- Analysis of a token (or of one of its segments) by a source. segment is 1 for
-- unsegmented tokens; Hebrew prefixes and suffixes will use further segments.
CREATE TABLE token_analyses (
    token_id          BIGINT NOT NULL REFERENCES tokens (id) ON DELETE CASCADE,
    segment           INT NOT NULL DEFAULT 1 CHECK (segment > 0),
    source_record_id  INT NOT NULL REFERENCES source_records (id),
    lexeme_id         INT NOT NULL REFERENCES lexemes (id),
    part_of_speech    TEXT NOT NULL,
    morph_code        TEXT,
    person            TEXT,
    number            TEXT,
    gender            TEXT,
    grammatical_case  TEXT,
    tense             TEXT,
    voice             TEXT,
    mood              TEXT,
    degree            TEXT,
    strong            TEXT,
    gloss             TEXT,
    PRIMARY KEY (token_id, segment, source_record_id)
);

CREATE INDEX token_analyses_lexeme_idx ON token_analyses (lexeme_id);
