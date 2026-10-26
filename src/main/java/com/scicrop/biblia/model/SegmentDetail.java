package com.scicrop.biblia.model;

import java.util.List;

/** Analysis of one segment of a token, with the counts and the most common English glosses of its lemma in the
 *  edition. */
public record SegmentDetail(
        int segment,
        String segmentText,
        String languageCode,
        int lexemeId,
        String lemma,
        String partOfSpeech,
        String morphCode,
        String person,
        String number,
        String gender,
        String grammaticalCase,
        String tense,
        String voice,
        String mood,
        String degree,
        String stem,
        String verbForm,
        String state,
        String strong,
        String gloss,
        String transliteration,
        OccurrenceCounts lemmaCounts,
        List<String> lemmaGlosses) {

    public SegmentDetail withLemma(OccurrenceCounts counts, List<String> glosses) {
        return new SegmentDetail(segment, segmentText, languageCode, lexemeId, lemma, partOfSpeech, morphCode, person,
                number, gender, grammaticalCase, tense, voice, mood, degree, stem, verbForm, state, strong, gloss,
                transliteration, counts, glosses);
    }
}
