package com.scicrop.biblia.model;

import java.util.List;

/** A lemma whose English glosses contain the searched word, with how many segments carry those glosses. */
public record GlossMatch(
        int lexemeId,
        String lemma,
        String languageCode,
        List<String> matchedGlosses,
        long matchedOccurrences,
        long lemmaVerses) {
}
