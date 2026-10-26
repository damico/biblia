package com.scicrop.biblia.model;

/**
 * A verse holding occurrences on one axis.
 * key is the Greek Strong number that links points in the Septuagint mode, null otherwise.
 */
public record LinkPoint(
        String bookCode,
        int chapter,
        int verse,
        long count,
        String key) {
}
