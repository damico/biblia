package com.scicrop.biblia.model;

/** Provenance of imported or derived data, as recorded in source_records. */
public record SourceRecord(
        String name,
        String maintainer,
        String url,
        String versionRef,
        String license,
        String attribution) {

    /** A 40 character git commit is shortened to 7; other version labels are kept. */
    public String shortVersion() {
        return versionRef.matches("[0-9a-f]{40}") ? "commit " + versionRef.substring(0, 7) : versionRef;
    }
}
