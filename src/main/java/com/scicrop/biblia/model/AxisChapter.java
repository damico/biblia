package com.scicrop.biblia.model;

/** A chapter on a visualization axis; its height is proportional to its number of verses. */
public record AxisChapter(
        String bookCode,
        String bookName,
        int chapter,
        int verses) {
}
