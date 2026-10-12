package com.sentinel.domain.review;

/**
 * One problem found by a review.
 *
 * @param line       last line the finding refers to, or null when it applies to the code as a whole
 * @param startLine  first line of a multi-line range, or null for a single line
 * @param suggestion exact replacement for the affected lines, or null when there is no safe fix
 */
public record Finding(
        Severity severity,
        Integer line,
        Integer startLine,
        String title,
        String explanation,
        String suggestion) {

    /** Whether the finding covers more than one line. */
    public boolean spansLines() {
        return line != null && startLine != null && startLine < line;
    }
}
