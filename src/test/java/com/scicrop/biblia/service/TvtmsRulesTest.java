package com.scicrop.biblia.service;

import com.scicrop.biblia.model.StandardRef;
import com.scicrop.biblia.model.VerseStat;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TvtmsRulesTest {

    /** Psalm 3 with n verses of 5 words; title before verse 1 when titled is set. */
    private static TvtmsRules.Edition psalm3(int verses, boolean titled) {
        List<VerseStat> stats = new ArrayList<>();
        for (int v = 1; v <= verses; v++) {
            stats.add(new VerseStat(v, "PSA", 3, v, 5, titled && v == 1));
        }
        return new TvtmsRules.Edition(stats);
    }

    // Rules copied from TVTMS (Expanded section).
    private static final TvtmsRules.Rule HEBREW_TITLE = new TvtmsRules.Rule("Hebrew+Latin+Greek", "Psa.3:1",
            "Psa.3:Title", "Renumber title", "Psa.3:1=Exist & Psa.3:9=Last & Psa.3:TextBeforeV1=NotExist");
    private static final TvtmsRules.Rule ENGLISH_TITLE = new TvtmsRules.Rule("EngTitleSeparate", "Psa.3:1",
            "Psa.3:1", "Keep verse", "Psa.3:1=Exist & Psa.3:8=Last & Psa.3:TextBeforeV1=Exist");

    @Test
    void hebrewPsalmTitleIsRecognisedByVerseCountAndMissingHeading() {
        assertTrue(TvtmsRules.applies(HEBREW_TITLE, psalm3(9, false)));
        assertFalse(TvtmsRules.applies(HEBREW_TITLE, psalm3(8, true)));
    }

    @Test
    void portuguesePsalmWithHeadingKeepsItsNumbers() {
        assertTrue(TvtmsRules.applies(ENGLISH_TITLE, psalm3(8, true)));
        assertFalse(TvtmsRules.applies(ENGLISH_TITLE, psalm3(9, false)));
    }

    @Test
    void wordCountComparisonsUseMultipliersAndSums() {
        List<VerseStat> stats = List.of(
                new VerseStat(1, "MAL", 4, 4, 10, false),
                new VerseStat(2, "MAL", 4, 5, 4, false),
                new VerseStat(3, "MAL", 4, 6, 12, false));
        TvtmsRules.Rule rule = new TvtmsRules.Rule("Eng-KJV+Latin", "Mal.4:1", "Mal.4:1", "Keep verse",
                "Mal.4:5*2<Mal.4:4+Mal.4:6");
        assertTrue(TvtmsRules.applies(rule, new TvtmsRules.Edition(stats)));
    }

    @Test
    void readsStandardReferencesWithTitlesPartsRangesAndInheritedBook() {
        assertEquals(List.of(new StandardRef("PSA", 3, 0, "")), TvtmsRules.standardRefs("Psa.3:Title"));
        assertEquals(List.of(new StandardRef("3JN", 1, 14, "b")), TvtmsRules.standardRefs("3Jn.1:14!b"));
        assertEquals(List.of(new StandardRef("ACT", 19, 40, ""), new StandardRef("ACT", 19, 41, "")),
                TvtmsRules.standardRefs("Act.19:40-41"));
        assertEquals(List.of(new StandardRef("ISA", 63, 19, ""), new StandardRef("ISA", 64, 1, "")),
                TvtmsRules.standardRefs("Isa.63:19; 64:1"));
        assertEquals(List.of(), TvtmsRules.standardRefs("Absent [=Isa.63:19]"));
    }

    @Test
    void bookCodesMatchUsfmCodes() {
        assertEquals("Gen", TvtmsRules.tvtmsBook("GEN"));
        assertEquals("1Sa", TvtmsRules.tvtmsBook("1SA"));
        assertEquals("3Jn", TvtmsRules.tvtmsBook("3JN"));
    }
}
