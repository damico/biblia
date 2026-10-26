package com.scicrop.biblia.dto;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Fields accepted by the concordance endpoint. Exactly one of lexeme or form is required;
 * the remaining fields narrow the search.
 */
public record ConcordanceQuery(
        Integer lexeme,
        String form,
        String book,
        String person,
        String number,
        String gender,
        String grammaticalCase,
        String tense,
        String voice,
        String mood,
        String stem,
        String verbForm,
        String state,
        Integer offset,
        Integer limit) {

    public static final int DEFAULT_LIMIT = 100;
    public static final int MAX_LIMIT = 500;

    public static ConcordanceQuery ofLexeme(int lexemeId) {
        return new ConcordanceQuery(lexemeId, null, null, null, null, null, null, null, null, null, null, null, null,
                null, null);
    }

    public static ConcordanceQuery ofForm(String form) {
        return new ConcordanceQuery(null, form, null, null, null, null, null, null, null, null, null, null, null,
                null, null);
    }

    /** Morphological filters that were given, keyed by query parameter name. */
    public Map<String, String> morphFilters() {
        Map<String, String> filters = new LinkedHashMap<>();
        put(filters, "person", person);
        put(filters, "number", number);
        put(filters, "gender", gender);
        put(filters, "grammaticalCase", grammaticalCase);
        put(filters, "tense", tense);
        put(filters, "voice", voice);
        put(filters, "mood", mood);
        put(filters, "stem", stem);
        put(filters, "verbForm", verbForm);
        put(filters, "state", state);
        return filters;
    }

    public int effectiveOffset() {
        return offset == null || offset < 0 ? 0 : offset;
    }

    public int effectiveLimit() {
        return limit == null || limit <= 0 ? DEFAULT_LIMIT : Math.min(limit, MAX_LIMIT);
    }

    private static void put(Map<String, String> filters, String name, String value) {
        if (value != null && !value.isBlank()) {
            filters.put(name, value.strip());
        }
    }
}
