package com.scicrop.biblia.model;

/** What the versification tests need to know about one passage of an edition. */
public record VerseStat(
        long passageId,
        String usfmCode,
        int chapter,
        int verse,
        int words,
        boolean hasHeading) {
}
