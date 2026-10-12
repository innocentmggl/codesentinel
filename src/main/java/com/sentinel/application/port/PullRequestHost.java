package com.sentinel.application.port;

import com.sentinel.domain.scm.PullRequest;
import com.sentinel.domain.scm.ReviewSubmission;

public interface PullRequestHost {

    /** Whether this host recognises the pull request URL. */
    boolean supports(String url);

    /** Loads the pull request metadata and the diff of every changed file. */
    PullRequest fetch(String url);

    /**
     * Publishes a review on the pull request.
     *
     * @return link to the published review
     */
    String submit(PullRequest pullRequest, ReviewSubmission review);
}
