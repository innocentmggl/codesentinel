package com.sentinel.domain.scm;

import com.sentinel.domain.review.Severity;

/**
 * A review comment anchored to lines of the new version of a file.
 *
 * @param startLine first line of the commented range; equal to {@code line} for a single-line comment
 * @param line      last line of the commented range
 * @param body      Markdown text of the comment
 */
public record ReviewComment(Severity severity, String path, int startLine, int line, String body) {
}
