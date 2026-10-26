-- Experimental word alignment of a translation without tokens (the Biblia Livre) to an original
-- language edition, computed by this project (scripts/align-blivre.sh). Each row links one word of
-- passages.content, by code point offsets, to one original token. These are statistical suggestions,
-- not a reviewed alignment: the interface always presents them as a test.

CREATE TABLE suggested_alignments (
    source_record_id   INT NOT NULL REFERENCES source_records (id),
    passage_id         BIGINT NOT NULL REFERENCES passages (id) ON DELETE CASCADE,
    start_offset       INT NOT NULL CHECK (start_offset >= 0),
    end_offset         INT NOT NULL,
    original_token_id  BIGINT NOT NULL REFERENCES tokens (id) ON DELETE CASCADE,
    confidence         REAL NOT NULL CHECK (confidence > 0 AND confidence <= 1),
    PRIMARY KEY (passage_id, start_offset, original_token_id),
    CHECK (end_offset > start_offset)
);

-- Deleting the tokens of an original edition (import --replace) looks rows up by token.
CREATE INDEX suggested_alignments_original_idx ON suggested_alignments (original_token_id);

INSERT INTO source_records (name, maintainer, url, version_ref, obtained_on, license, attribution,
                            coverage, modifications, known_limitations)
VALUES (
    'Alinhamento automático Bíblia Livre - originais (teste)',
    'Este projeto (Jose Damico)',
    'https://josedamico.com.br/biblia',
    'ibm1-diagonal-1',
    DATE '2026-10-26',
    'Dados derivados da Bíblia Livre (CC BY 3.0 BR) e do MACULA (CC BY 4.0), sob as mesmas licenças',
    'Sugestões de alinhamento palavra a palavra calculadas automaticamente por este projeto, em teste e sem '
        || 'revisão humana, entre a Bíblia Livre e os textos originais do MACULA (WLC no Antigo Testamento, '
        || 'Nestle 1904 no Novo). Método estatístico: IBM Model 1 com preferência pela diagonal, como no '
        || 'fast_align (Dyer, Chahuneau e Smith, 2013), treinado nos dois sentidos.',
    'Versículos da Bíblia Livre com correspondência exata (um para um) de versículo no original.',
    'Só ficam os pares que os dois sentidos do modelo escolhem e com confiança mínima; palavras marcadas como '
        || 'acrescentadas pela tradução nunca são alinhadas.',
    'Teste: estatística de coocorrência, não tradução nem revisão. Pode ligar palavras erradas, sobretudo '
        || 'palavras raras, expressões idiomáticas e trechos com ordem muito diferente; muitas palavras ficam '
        || 'sem sugestão.'
);
