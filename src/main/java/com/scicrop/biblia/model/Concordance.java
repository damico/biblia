package com.scicrop.biblia.model;

import java.util.List;
import java.util.Map;

public record Concordance(
        Edition edition,
        Integer lexemeId,
        String lemma,
        List<String> lemmaGlosses,
        String form,
        String bookCode,
        Map<String, String> filters,
        OccurrenceCounts counts,
        List<BookCount> byBook,
        List<ConcordanceHit> hits,
        int offset,
        int limit) {
}
