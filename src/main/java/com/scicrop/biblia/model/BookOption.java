package com.scicrop.biblia.model;

/** A book that can be chosen for an axis, with its number of occurrences there (0 when not counted). */
public record BookOption(
        String bookCode,
        String bookName,
        String testament,
        long occurrences) {
}
