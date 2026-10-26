package com.scicrop.biblia.model;

/** Occurrences are tokens; verses and books are distinct passages and books containing them. */
public record OccurrenceCounts(
        long occurrences,
        long verses,
        long books) {
}
