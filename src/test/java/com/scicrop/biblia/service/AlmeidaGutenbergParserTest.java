package com.scicrop.biblia.service;

import com.scicrop.biblia.model.ParsedPassage;
import com.scicrop.biblia.model.PassageAnnotation;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AlmeidaGutenbergParserTest {

    // Two books in the layout of the Gutenberg file: drop cap chapter numbers, cross references after
    // each chapter, lettered notes at the end, italics, a verse typeset inside the previous one,
    // a psalm title and the editorial lines that are skipped.
    private static final String TEXT = """
            *** START OF THE PROJECT GUTENBERG EBOOK A BIBLIA SAGRADA ***

            INDICE

            O PRIMEIRO LIVRO DE MOYSES CHAMADO GENESIS.




            _A creação do ceu e da terra._

            [Antes de Christo 4004]

            1 No [1] principio creou [A] Deus os céus e a terra.

            2 E a terra era sem fórma e vasia; e _havia_ trevas. 3 E disse Deus:
            Haja luz [2]; e houve luz.

            4 E viu Deus que era boa a luz.

            [1] Pro. 8.23.

            [2] II Cor. 4.6.




            _O descanço._

            2 Assim os céus foram acabados.

            2 E descançou (_no_ [1] setimo dia).

            [1] Exo. 20.11.




            O LIVRO DOS PSALMOS.




            _A felicidade dos justos._

            Psalmo [B] de David.

            1 Bemaventurado o varão.

            FIM DO VELHO TESTAMENTO.

            NOTAS

            [A] ou, fez.

            [B] Heb. canção.

            *** END OF THE PROJECT GUTENBERG EBOOK ***
            """;

    private static List<ParsedPassage> parse() {
        return AlmeidaGutenbergParser.parse(TEXT, List.of("GEN", "PSA")).passages();
    }

    @Test
    void readsBooksChaptersAndVerses() {
        List<ParsedPassage> passages = parse();
        assertEquals(List.of("GEN 1:1", "GEN 1:2", "GEN 1:3", "GEN 1:4", "GEN 2:1", "GEN 2:2", "PSA 1:1"),
                passages.stream().map(ParsedPassage::reference).toList());
        assertEquals("No principio creou Deus os céus e a terra.", passages.get(0).content());
        assertEquals("Assim os céus foram acabados.", passages.get(4).content());
    }

    @Test
    void splitsAVerseTypesetInsideThePreviousOne() {
        List<ParsedPassage> passages = parse();
        assertEquals("E a terra era sem fórma e vasia; e havia trevas.", passages.get(1).content());
        assertEquals("E disse Deus: Haja luz; e houve luz.", passages.get(2).content());
    }

    @Test
    void turnsMarkersAndItalicsIntoAnnotations() {
        List<ParsedPassage> passages = parse();
        assertEquals(List.of(
                        new PassageAnnotation("crossref", 3, 3, null, "Pro. 8.23."),
                        new PassageAnnotation("footnote", 19, 19, null, "ou, fez.")),
                passages.get(0).annotations());
        assertEquals(List.of(new PassageAnnotation("added", 35, 40, null, null)), passages.get(1).annotations());
        assertEquals(List.of(new PassageAnnotation("crossref", 22, 22, null, "II Cor. 4.6.")),
                passages.get(2).annotations());
        // cross references are numbered per chapter
        ParsedPassage rest = passages.get(5);
        assertEquals("E descançou (no setimo dia).", rest.content());
        assertEquals(List.of(
                        new PassageAnnotation("added", 13, 15, null, null),
                        new PassageAnnotation("crossref", 16, 16, null, "Exo. 20.11.")),
                rest.annotations());
    }

    @Test
    void keepsPsalmTitlesAsHeadingsAndSkipsEditorialLines() {
        List<ParsedPassage> passages = parse();
        assertNull(passages.get(0).heading());
        ParsedPassage psalm = passages.get(6);
        assertEquals("Psalmo de David.", psalm.heading());
        assertEquals(List.of(new PassageAnnotation("footnote", 0, 0, "Título", "Heb. canção.")),
                psalm.annotations());
    }

    @Test
    void rejectsAMissingCrossReference() {
        String broken = TEXT.replace("[1] Pro. 8.23.", "");
        assertThrows(IllegalArgumentException.class,
                () -> AlmeidaGutenbergParser.parse(broken, List.of("GEN", "PSA")));
    }
}
