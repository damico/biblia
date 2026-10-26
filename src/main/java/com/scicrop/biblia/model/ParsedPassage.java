package com.scicrop.biblia.model;

import java.util.List;

public record ParsedPassage(
        String usfmCode,
        int chapter,
        int verse,
        String heading,
        String content,
        List<PassageAnnotation> annotations) {

    public String reference() {
        return usfmCode + " " + chapter + ":" + verse;
    }
}
