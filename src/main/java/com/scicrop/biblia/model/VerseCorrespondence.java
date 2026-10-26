package com.scicrop.biblia.model;

import java.util.List;

/**
 * The passages of a target edition that hold the text of one source verse.
 * method is "standard" (through the standard versification) or "reference" (same chapter and verse,
 * used only when the standard references were not computed). exact is set when one target verse holds
 * exactly the text of the source verse; otherwise the texts are divided differently (or there is none).
 */
public record VerseCorrespondence(
        Edition targetEdition,
        String method,
        boolean exact,
        List<Passage> passages) {
}
