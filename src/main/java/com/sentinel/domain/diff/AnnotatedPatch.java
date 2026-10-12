package com.sentinel.domain.diff;

import java.util.Set;
import java.util.stream.IntStream;

/**
 * A file diff prepared for a language model.
 *
 * @param numberedText     the diff with the new-file line number in front of every surviving line
 * @param commentableLines new-file lines a review comment may be attached to (added and context lines)
 * @param addedLines       new-file lines added by the change
 * @param addedCode        the added lines without diff markers; used as the retrieval query
 */
public record AnnotatedPatch(
        String numberedText,
        Set<Integer> commentableLines,
        Set<Integer> addedLines,
        String addedCode) {

    /** Whether a comment may be attached to this line of the new file. */
    public boolean accepts(int line) {
        return commentableLines.contains(line);
    }

    /** Whether a comment may span from {@code first} to {@code last}, inclusive. */
    public boolean accepts(int first, int last) {
        return first <= last && IntStream.rangeClosed(first, last).allMatch(commentableLines::contains);
    }
}
