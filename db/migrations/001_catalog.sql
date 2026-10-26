-- Core catalog: provenance, languages, works, editions, canons, books and passages.
-- Tokens, lexemes and alignments come in later scripts.

CREATE TABLE source_records (
    id                 INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name               TEXT NOT NULL,
    maintainer         TEXT,
    url                TEXT NOT NULL,
    version_ref        TEXT NOT NULL,
    obtained_on        DATE NOT NULL,
    license            TEXT NOT NULL,
    attribution        TEXT NOT NULL,
    coverage           TEXT,
    modifications      TEXT,
    known_limitations  TEXT,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (url, version_ref)
);

-- ISO 639-3 codes (hbo, arc, grc, syc, por)
CREATE TABLE languages (
    code              TEXT PRIMARY KEY CHECK (code ~ '^[a-z]{3}$'),
    name              TEXT NOT NULL,
    script_direction  TEXT NOT NULL CHECK (script_direction IN ('ltr', 'rtl'))
);

CREATE TABLE textual_works (
    id           INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code         TEXT NOT NULL UNIQUE,
    name         TEXT NOT NULL,
    description  TEXT
);

CREATE TABLE editions (
    id                  INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code                TEXT NOT NULL UNIQUE,
    textual_work_id     INT NOT NULL REFERENCES textual_works (id),
    language_code       TEXT NOT NULL REFERENCES languages (code),
    source_record_id    INT NOT NULL REFERENCES source_records (id),
    name                TEXT NOT NULL,
    version             TEXT NOT NULL,
    editorial_criteria  TEXT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE canons (
    id           INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code         TEXT NOT NULL UNIQUE,
    name         TEXT NOT NULL,
    description  TEXT
);

-- Book identity, independent of any edition. code follows OSIS (Gen, Matt, 1Cor).
CREATE TABLE books (
    id         INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code       TEXT NOT NULL UNIQUE,
    testament  TEXT NOT NULL CHECK (testament IN ('OT', 'NT'))
);

CREATE TABLE canon_books (
    canon_id      INT NOT NULL REFERENCES canons (id),
    book_id       INT NOT NULL REFERENCES books (id),
    position      INT NOT NULL CHECK (position > 0),
    name          TEXT NOT NULL,
    abbreviation  TEXT NOT NULL,
    PRIMARY KEY (canon_id, book_id),
    UNIQUE (canon_id, position),
    UNIQUE (canon_id, abbreviation)
);

-- Reference unit of an edition. Numbering is the edition's own; mapping between
-- editions belongs to passage_correspondences (later script).
CREATE TABLE passages (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    edition_id   INT NOT NULL REFERENCES editions (id),
    book_id      INT NOT NULL REFERENCES books (id),
    chapter      INT NOT NULL CHECK (chapter >= 0),
    verse        INT NOT NULL CHECK (verse >= 0),
    subdivision  TEXT NOT NULL DEFAULT '',
    sort_order   INT NOT NULL,
    content      TEXT NOT NULL,
    UNIQUE (edition_id, book_id, chapter, verse, subdivision),
    UNIQUE (edition_id, sort_order)
);

CREATE INDEX passages_book_chapter_idx ON passages (edition_id, book_id, chapter);
