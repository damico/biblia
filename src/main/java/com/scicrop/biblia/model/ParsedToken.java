package com.scicrop.biblia.model;

import java.util.List;

/** One word of a source file with its analyses (none for translations), before being stored. */
public record ParsedToken(
        String sourceRef,
        int position,
        String surface,
        String trailing,
        String normalized,
        List<ParsedAnalysis> analyses) {
}
