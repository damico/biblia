package com.scicrop.biblia.model;

/**
 * A reference in the standard versification. verse 0 is a psalm title; part is a subverse letter
 * (a, b, ...) or empty for the whole verse. usfmCode is the book code used in the database.
 */
public record StandardRef(
        String usfmCode,
        int chapter,
        int verse,
        String part) {
}
