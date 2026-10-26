package com.scicrop.biblia.model;

public record SearchVerse(
        String bookCode,
        String bookName,
        String testament,
        int chapter,
        int verse,
        String content) {
}
