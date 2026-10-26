package com.scicrop.biblia.model;

/**
 * Analysis of one segment of a word, before being stored. segmentText is null when the analysis
 * covers the whole word (Greek); Hebrew and Aramaic words have one analysis per prefix, stem and suffix.
 * lxxGreek and lxxStrong are the Septuagint equivalent of a Hebrew or Aramaic segment, when known.
 */
public record ParsedAnalysis(
        int segment,
        String segmentText,
        String languageCode,
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
        String lxxGreek,
        String lxxStrong) {
}
