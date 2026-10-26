package com.scicrop.biblia.service;

import com.scicrop.biblia.model.ParsedPassage;
import com.scicrop.biblia.model.PassageAnnotation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class BlivreParserTest {

    private static final String HEADER = """
            \\name-long
            Livro
            \\*name-long
            \\ubs-code
            PSA
            \\*ubs-code
            """;

    private static BlivreParser.Result parse(String body) {
        return BlivreParser.parse("test.txt", HEADER + body);
    }

    private static String spanText(ParsedPassage p, PassageAnnotation a) {
        int start = p.content().offsetByCodePoints(0, a.startOffset());
        int end = p.content().offsetByCodePoints(0, a.endOffset());
        return p.content().substring(start, end);
    }

    @Test
    void joinsLinesAndRecordsAddedWords() {
        ParsedPassage p = parse("""
                \\v Sl.1.1
                apresentar holocaustos
                \\added
                conforme
                \\*added
                 o numero de
                todos eles.
                """).passages().getFirst();

        assertEquals("apresentar holocaustos conforme o numero de todos eles.", p.content());
        PassageAnnotation added = p.annotations().getFirst();
        assertEquals("added", added.kind());
        assertEquals("conforme", spanText(p, added));
    }

    @Test
    void noSpaceBeforePunctuationAfterSpan() {
        ParsedPassage p = parse("""
                \\v Sl.1.1
                E
                \\added
                Jesus
                \\*added
                , olhando, viu.
                """).passages().getFirst();

        assertEquals("E Jesus, olhando, viu.", p.content());
    }

    @Test
    void footnoteWithKeyIsAnchoredAtCurrentPosition() {
        ParsedPassage p = parse("""
                \\v Sl.1.1
                os filhos de Deus
                \\fn
                \\key
                filhos de Deus
                \\*key
                i.e., os anjos
                \\*fn
                vieram.
                """).passages().getFirst();

        assertEquals("os filhos de Deus vieram.", p.content());
        PassageAnnotation note = p.annotations().getFirst();
        assertEquals("footnote", note.kind());
        assertEquals("os filhos de Deus".length(), note.startOffset());
        assertEquals("filhos de Deus", note.keyText());
        assertEquals("i.e., os anjos", note.body());
    }

    @Test
    void psalmTitleBecomesHeadingOfNextVerse() {
        BlivreParser.Result r = parse("""
                \\v Sl.2.12
                Bem-aventurados todos.
                \\psalm-title
                Salmo de Davi.
                \\*psalm-title
                \\v Sl.3.1
                Ah SENHOR!
                """);

        assertNull(r.passages().get(0).heading());
        assertEquals("Bem-aventurados todos.", r.passages().get(0).content());
        assertEquals("Salmo de Davi.", r.passages().get(1).heading());
        assertEquals(0, r.repairs());
    }

    @Test
    void repairsFootnoteClosedWithOpeningMarker() {
        BlivreParser.Result r = parse("""
                \\v Sl.1.1
                e Jesus,
                \\fn
                Lit. ele
                \\*fn
                 tomando aquele homem,
                \\fn
                Lit. tomando-o
                \\fn
                 curou-o.
                """);

        ParsedPassage p = r.passages().getFirst();
        assertEquals("e Jesus, tomando aquele homem, curou-o.", p.content());
        assertEquals(2, p.annotations().size());
        assertEquals("Lit. tomando-o", p.annotations().get(1).body());
        assertEquals(1, r.repairs());
    }

    @Test
    void repairsMarkerWithoutBackslash() {
        BlivreParser.Result r = parse("""
                \\v Sl.1.1
                E
                \\added
                Jesus
                *added
                , olhando.
                """);

        assertEquals("E Jesus, olhando.", r.passages().getFirst().content());
        assertEquals(1, r.passages().getFirst().annotations().size());
        assertEquals(1, r.repairs());
    }
}
