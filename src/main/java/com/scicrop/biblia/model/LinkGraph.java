package com.scicrop.biblia.model;

import java.util.List;

/**
 * Occurrences to link on two axes, one point per verse. Modes:
 * fan   - from the origin verse to every occurrence of the lemma on the right axis;
 * pairs - every occurrence on the left axis to every occurrence on the right axis;
 * lxx   - occurrences of the lemma to verses of the other testament whose word has the same Greek
 *         Strong number (the point key) in the Septuagint (Hebrew and Aramaic side) or in the New Testament.
 * The lines are built by the browser for the visible part of each axis; pairCount is the total number
 * of occurrence pairs.
 */
public record LinkGraph(
        String mode,
        String leftEditionCode,
        String leftEditionName,
        String rightEditionCode,
        String rightEditionName,
        String leftRange,
        String rightRange,
        LinkPoint origin,
        List<String> strongs,
        List<AxisChapter> leftAxis,
        List<AxisChapter> rightAxis,
        List<LinkPoint> leftPoints,
        List<LinkPoint> rightPoints,
        long pairCount,
        List<BookOption> leftBooks,
        List<BookOption> rightBooks) {
}
