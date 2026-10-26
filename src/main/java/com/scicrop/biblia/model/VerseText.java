package com.scicrop.biblia.model;

import java.util.List;

/** Text of a verse of an edition and the corresponding verses of a translation (verse level only). */
public record VerseText(
        String bookCode,
        String bookName,
        int chapter,
        int verse,
        String content,
        List<ParallelVerse> translation) {
}
