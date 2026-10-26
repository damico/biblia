package com.scicrop.biblia.model;

import java.util.List;

/** A token of an original language edition with one analysis per segment. */
public record TokenDetail(
        long id,
        String editionCode,
        String editionName,
        String languageCode,
        String bookCode,
        String bookName,
        int chapter,
        int verse,
        String surface,
        String normalized,
        List<SegmentDetail> segments,
        OccurrenceCounts formCounts) {
}
