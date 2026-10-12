package com.sentinel.domain.scm;

import java.util.List;

/**
 * @param headSha commit the review comments are attached to
 */
public record PullRequest(
        PullRequestRef ref,
        String title,
        String description,
        String headSha,
        List<ChangedFile> files) {

    public PullRequest {
        files = List.copyOf(files);
    }
}
