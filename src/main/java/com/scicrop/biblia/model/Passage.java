package com.scicrop.biblia.model;

import java.util.List;

public record Passage(
        int chapter,
        int verse,
        String heading,
        String content,
        List<PassageAnnotation> annotations,
        List<Token> tokens) {
}
