package com.scicrop.biblia.service;

import com.scicrop.biblia.model.ParsedToken;
import com.scicrop.biblia.model.ParsedVerse;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class MaculaParserTest {

    private static final String HEBREW_HEADER = "xml:id\tref\ttext\tafter\tlemma\tpos\tmorph\tlang\tperson\tgender\tnumber"
            + "\tstate\tstem\ttype\tstrongnumberx\tenglish\ttransliteration\tgreek\tgreekstrong";

    private static String hebrewRow(String id, String ref, String text, String after, String lemma, String pos,
                                    String lang) {
        return String.join("\t", id, ref, text, after, lemma, pos, "", lang, "", "", "", "", "", "", "", "", "",
                "\u1F10\u03BD", "1722");
    }

    @Test
    void groupsHebrewSegmentsIntoOneTokenPerWord() {
        List<String> lines = new ArrayList<>();
        lines.add(HEBREW_HEADER);
        // "bereshit" is a prefix and a stem sharing the ref of the word.
        lines.add(hebrewRow("o010010010011", "GEN 1:1!1", "בְּ", "", "בְּ",
                "preposition", "H"));
        lines.add(hebrewRow("o010010010012", "GEN 1:1!1", "רֵאשִׁ֖ית",
                " ", "רֵאשִׁית", "noun", "H"));
        lines.add(hebrewRow("o010010010021", "GEN 1:1!2", "בָּרָא", "׃",
                "בָּרָא", "verb", "A"));

        List<ParsedVerse> verses = MaculaParser.parse(lines);

        assertEquals(1, verses.size());
        List<ParsedToken> tokens = verses.getFirst().tokens();
        assertEquals(2, tokens.size());
        ParsedToken first = tokens.getFirst();
        assertEquals("o01001001001", first.sourceRef());
        assertEquals(2, first.analyses().size());
        assertEquals("hbo", first.analyses().get(0).languageCode());
        assertEquals(2, first.analyses().get(1).segment());
        assertEquals("arc", tokens.get(1).analyses().getFirst().languageCode());
        assertEquals("1722", first.analyses().getFirst().lxxStrong());
        // The cantillation mark (U+0596) is dropped from the normalized form, vowels are kept.
        assertEquals("בְּרֵאשִׁית", first.normalized());
    }

    @Test
    void maqafJoinsWordsWithoutSpace() {
        List<String> lines = new ArrayList<>();
        lines.add(HEBREW_HEADER);
        lines.add(hebrewRow("o010010070011", "GEN 1:7!1", "אֶת", "־", "אֵת",
                "particle", "H"));
        lines.add(hebrewRow("o010010070021", "GEN 1:7!2", "כָּל", "׃",
                "כֹּל", "noun", "H"));

        ParsedVerse verse = MaculaParser.parse(lines).getFirst();

        assertEquals("אֶת־כָּל׃", verse.passage().content());
    }

    @Test
    void greekWordHasOneAnalysisCoveringTheWholeToken() {
        List<String> lines = List.of(
                "xml:id\tref\tclass\ttext\tafter\tlemma\tnormalized\tstrong\tmorph\tperson\tnumber\tgender"
                        + "\tcase\ttense\tvoice\tmood\tdegree\tgloss",
                "n43001001001\tJHN 1:1!1\tprep\tἘν\t \tἐν\tἘν\t1722\tPREP"
                        + "\t\t\t\t\t\t\t\t\tIn");

        ParsedToken token = MaculaParser.parse(lines).getFirst().tokens().getFirst();

        assertEquals(1, token.analyses().size());
        assertNull(token.analyses().getFirst().segmentText());
        assertEquals("grc", token.analyses().getFirst().languageCode());
    }
}
