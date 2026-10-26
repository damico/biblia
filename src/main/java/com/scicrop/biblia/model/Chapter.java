package com.scicrop.biblia.model;

import java.util.List;

public record Chapter(
        Edition edition,
        EditionBook book,
        int chapter,
        List<Passage> passages) {
}
