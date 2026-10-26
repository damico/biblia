package com.scicrop.biblia.dto;

/** Field accepted by the word search endpoint: one word or a short phrase, in Portuguese or English. */
public record SearchQuery(String q) {
}
