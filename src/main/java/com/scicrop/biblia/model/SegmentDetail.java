package com.scicrop.biblia.model;

/** Analysis of one segment of a token, with the counts of its lemma in the edition. */
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
        OccurrenceCounts lemmaCounts) {

    public SegmentDetail withLemmaCounts(OccurrenceCounts counts) {
        return new SegmentDetail(segment, segmentText, languageCode, lexemeId, lemma, partOfSpeech, morphCode, person,
                number, gender, grammaticalCase, tense, voice, mood, degree, stem, verbForm, state, strong, gloss,
                transliteration, counts);
    }
}
