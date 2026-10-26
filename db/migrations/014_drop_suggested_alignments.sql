-- The experimental word alignment of the Biblia Livre (013) was withdrawn after review: its quality
-- was not good enough to show. Its rows and its source record are removed.

DROP TABLE suggested_alignments;

DELETE FROM source_records
 WHERE url = 'https://josedamico.com.br/biblia' AND version_ref = 'ibm1-diagonal-1';
