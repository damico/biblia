-- Deleting tokens of an original language edition checks token_alignments by original_token_id;
-- without this index every deleted token scans the whole table (a WLC reload took 29 minutes).

CREATE INDEX token_alignments_original_idx ON token_alignments (original_token_id);
