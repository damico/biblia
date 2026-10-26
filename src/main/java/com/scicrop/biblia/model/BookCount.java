package com.scicrop.biblia.model;

public record BookCount(
        String bookCode,
        String bookName,
        long occurrences,
        long verses) {
}
