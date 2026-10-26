package com.scicrop.biblia.model;

public record EditionBook(
        String code,
        String testament,
        String name,
        String abbreviation,
        int chapterCount) {
}
