package com.scicrop.biblia.model;

import java.util.List;

/**
 * alignedTo holds the ids of original language tokens linked to this token (translations only).
 * gloss is the short English gloss of the source, its segments joined in order (original languages only).
 */
public record Token(
        long id,
        int position,
        String surface,
        String trailing,
        List<Long> alignedTo,
        String gloss) {
}
