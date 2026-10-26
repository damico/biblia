package com.scicrop.biblia.model;

import java.util.List;

/**
 * Word search over both testaments: verses of the translation containing the word, original lemmas
 * associated with it by verse co-occurrence, and original lemmas whose English glosses contain it.
 */
public record SearchResult(
        String query,
        Edition translation,
        Edition oldTestament,
        Edition newTestament,
        long verseCount,
        List<SearchVerse> verses,
        List<LemmaAssociation> oldTestamentAssociations,
        List<LemmaAssociation> newTestamentAssociations,
        List<GlossMatch> oldTestamentGlosses,
        List<GlossMatch> newTestamentGlosses) {
}
