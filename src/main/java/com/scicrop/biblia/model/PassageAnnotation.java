package com.scicrop.biblia.model;

/**
 * Annotation anchored in a passage content by code point offsets (end exclusive).
 * kind is one of: added, footnote, reference.
 */
public record PassageAnnotation(
        String kind,
        int startOffset,
        int endOffset,
        String keyText,
        String body) {
}
