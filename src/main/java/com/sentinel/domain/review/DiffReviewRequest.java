package com.sentinel.domain.review;

import java.util.List;

import com.sentinel.domain.code.CodeChunk;

/**
 * Everything a reviewer needs to review one file of a pull request.
 *
 * @param numberedDiff the file's diff with new-file line numbers in front of every surviving line
 * @param relatedCode  code from the base project that is similar to the change, for context only
 * @param maxFindings  upper bound on the findings the reviewer should report
 */
public record DiffReviewRequest(
        String pullRequestTitle,
        String pullRequestDescription,
        String filePath,
        String numberedDiff,
        List<CodeChunk> relatedCode,
        int maxFindings) {
}
