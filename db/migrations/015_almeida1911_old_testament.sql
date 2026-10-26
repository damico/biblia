-- Old Testament of the Almeida 1911 (edition "jfa11"), from the Project Gutenberg transcription of the
-- 1911 Lisbon printing. The New Testament of the edition stays the Clear Bible Alignments text, aligned to
-- the SBLGNT. Verses are loaded by scripts/import-jfa11-ot.sh.

-- Cross references of the 1911 printing ("[1] Pro. 8.23."), shown as a list of passages, not as a quotation.
ALTER TABLE passage_annotations DROP CONSTRAINT passage_annotations_kind_check;
ALTER TABLE passage_annotations
    ADD CONSTRAINT passage_annotations_kind_check CHECK (kind IN ('added', 'footnote', 'reference', 'crossref'));

INSERT INTO source_records (name, maintainer, url, version_ref, obtained_on, license, attribution,
                            coverage, modifications, known_limitations)
VALUES (
    'A Biblia Sagrada (Almeida 1911), Project Gutenberg #62383',
    'Project Gutenberg; transcrição de Júlio Reis, abi278, Sergio Queiroz e Distributed Proofreaders',
    'https://www.gutenberg.org/ebooks/62383',
    'pg62383-2024-10-18',
    DATE '2026-10-26',
    'Domínio público',
    'A Biblia Sagrada, contendo o Velho e o Novo Testamento, traduzida em portuguez por João Ferreira '
        || 'd''Almeida, edição revista e corrigida, Deposito das Escripturas Sagradas, Lisboa, 1911 (domínio '
        || 'público). Transcrição do Project Gutenberg (eBook #62383, https://www.gutenberg.org/ebooks/62383), '
        || 'produzida por Júlio Reis, abi278, Sergio Queiroz e a Online Distributed Proofreading Team '
        || '(https://www.pgdp.net), com imagens de Faithofgod.net.',
    'Antigo Testamento (39 livros) do arquivo pg62383.txt, atualizado em 18/10/2024.',
    'Ortografia de 1911 mantida. Palavras em itálico (acrescentadas pelos tradutores) viram anotações; '
        || 'referências cruzadas numeradas e notas alternativas com letra viram notas do versículo; títulos dos '
        || 'Salmos e letras acrósticas de Lamentações viram cabeçalho do versículo. Resumos dos capítulos, '
        || 'subtítulos de seção e datas da cronologia ("Antes de Christo") são editoriais e não foram importados; '
        || 'uma marca de dúvida do transcritor (Nm 5:3) foi retirada.',
    'A nota do transcritor avisa que algumas referências cruzadas da edição impressa estão erradas. A '
        || 'versificação segue a da edição de 1911 (ex.: Jz 5:32, 1Sm 20:43, 1Rs 22:54).'
);

UPDATE editions
   SET editorial_criteria = 'Antigo Testamento do Project Gutenberg, com referências e notas da edição de 1911; '
                            || 'Novo Testamento com palavras alinhadas ao SBLGNT.'
 WHERE code = 'jfa11';
