package com.scicrop.biblia.model;

public record Edition(
        String code,
        String name,
        String version,
        String languageCode,
        String languageName,
        String scriptDirection,
        String sourceName,
        String sourceUrl,
        String sourceVersionRef,
        String license,
        String attribution) {
}
