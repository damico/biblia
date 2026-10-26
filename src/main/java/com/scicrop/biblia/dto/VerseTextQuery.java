package com.scicrop.biblia.dto;

/**
 * Fields accepted by the verse text endpoint: refs is a comma separated list of Book.Chapter.Verse
 * (book codes as in the books table, e.g. John.1.1,1Cor.13.4); translation is the edition whose
 * corresponding verses are returned with each verse.
 */
public record VerseTextQuery(
        String refs,
        String translation) {
}
