package com.scicrop.biblia.model;

import java.util.List;

/**
 * A verse containing at least one match. parallel holds the verses of the parallel edition that
 * correspond to it through the standard versification (by identical reference only when the standard
 * references were not computed); it is a verse level correspondence, never a word alignment.
 */
public record ConcordanceHit(
        String bookCode,
        String bookName,
        int chapter,
        int verse,
        List<Token> tokens,
        List<Integer> matchedPositions,
        String parallelEditionCode,
        List<ParallelVerse> parallel) {
}
