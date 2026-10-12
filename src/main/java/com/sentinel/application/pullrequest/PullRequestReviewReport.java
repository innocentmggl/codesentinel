package com.sentinel.application.pullrequest;

import java.util.List;

import com.sentinel.domain.scm.ReviewComment;

/**
 * @param posted       whether the review was published on the pull request
 * @param reviewUrl    link to the published review, null when nothing was posted
 * @param filesSkipped one entry per skipped file, with the reason
 */
public record PullRequestReviewReport(
        String pullRequest,
        boolean posted,
        String reviewUrl,
        int filesReviewed,
        List<String> filesSkipped,
        List<ReviewComment> comments) {
}
