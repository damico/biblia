package com.scicrop.biblia.model;

/** A verse of the parallel edition that corresponds (verse level) to a concordance hit. */
public record ParallelVerse(
        int chapter,
        int verse,
        String content) {
}
