-- USFM book codes, psalm headings and inline annotations of passages.

ALTER TABLE books ADD COLUMN usfm_code TEXT UNIQUE CHECK (usfm_code ~ '^[0-9A-Z]{3}$');

UPDATE books b
   SET usfm_code = v.usfm_code
  FROM (VALUES
        ('Gen', 'GEN'),
        ('Exod', 'EXO'),
        ('Lev', 'LEV'),
        ('Num', 'NUM'),
        ('Deut', 'DEU'),
        ('Josh', 'JOS'),
        ('Judg', 'JDG'),
        ('Ruth', 'RUT'),
        ('1Sam', '1SA'),
        ('2Sam', '2SA'),
        ('1Kgs', '1KI'),
        ('2Kgs', '2KI'),
        ('1Chr', '1CH'),
        ('2Chr', '2CH'),
        ('Ezra', 'EZR'),
        ('Neh', 'NEH'),
        ('Esth', 'EST'),
        ('Job', 'JOB'),
        ('Ps', 'PSA'),
        ('Prov', 'PRO'),
        ('Eccl', 'ECC'),
        ('Song', 'SNG'),
        ('Isa', 'ISA'),
        ('Jer', 'JER'),
        ('Lam', 'LAM'),
        ('Ezek', 'EZK'),
        ('Dan', 'DAN'),
        ('Hos', 'HOS'),
        ('Joel', 'JOL'),
        ('Amos', 'AMO'),
        ('Obad', 'OBA'),
        ('Jonah', 'JON'),
        ('Mic', 'MIC'),
        ('Nah', 'NAM'),
        ('Hab', 'HAB'),
        ('Zeph', 'ZEP'),
        ('Hag', 'HAG'),
        ('Zech', 'ZEC'),
        ('Mal', 'MAL'),
        ('Matt', 'MAT'),
        ('Mark', 'MRK'),
        ('Luke', 'LUK'),
        ('John', 'JHN'),
        ('Acts', 'ACT'),
        ('Rom', 'ROM'),
        ('1Cor', '1CO'),
        ('2Cor', '2CO'),
        ('Gal', 'GAL'),
        ('Eph', 'EPH'),
        ('Phil', 'PHP'),
        ('Col', 'COL'),
        ('1Thess', '1TH'),
        ('2Thess', '2TH'),
        ('1Tim', '1TI'),
        ('2Tim', '2TI'),
        ('Titus', 'TIT'),
        ('Phlm', 'PHM'),
        ('Heb', 'HEB'),
        ('Jas', 'JAS'),
        ('1Pet', '1PE'),
        ('2Pet', '2PE'),
        ('1John', '1JN'),
        ('2John', '2JN'),
        ('3John', '3JN'),
        ('Jude', 'JUD'),
        ('Rev', 'REV')
       ) AS v (code, usfm_code)
 WHERE b.code = v.code;

ALTER TABLE books ALTER COLUMN usfm_code SET NOT NULL;

-- Unnumbered heading shown before the passage (e.g. psalm titles in Portuguese numbering).
ALTER TABLE passages ADD COLUMN heading TEXT;

-- Annotations anchored in passages.content by code point offsets (0-based, end exclusive).
-- added: words supplied by the translator, spanning [start_offset, end_offset).
-- footnote / reference: anchored at start_offset = end_offset, text in body.
CREATE TABLE passage_annotations (
    passage_id    BIGINT NOT NULL REFERENCES passages (id) ON DELETE CASCADE,
    seq           INT NOT NULL,
    kind          TEXT NOT NULL CHECK (kind IN ('added', 'footnote', 'reference')),
    start_offset  INT NOT NULL CHECK (start_offset >= 0),
    end_offset    INT NOT NULL,
    key_text      TEXT,
    body          TEXT,
    PRIMARY KEY (passage_id, seq),
    CHECK (end_offset >= start_offset),
    CHECK ((kind = 'added') = (body IS NULL))
);
