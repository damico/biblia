package com.scicrop.biblia.model;

public record CanonBook(
        String code,
        String testament,
        int position,
        String name,
        String abbreviation) {
}
