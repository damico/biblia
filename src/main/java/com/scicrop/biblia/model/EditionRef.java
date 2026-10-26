package com.scicrop.biblia.model;

/** Internal identifiers of an edition, used by importers. */
public record EditionRef(int id, String languageCode, int sourceRecordId) {
}
