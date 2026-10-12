package com.sentinel.application.pullrequest;

import java.time.Instant;

public record ReviewJob(
        String id,
        String url,
        Status status,
        Instant submittedAt,
        Instant finishedAt,
        PullRequestReviewReport report,
        String error) {

    public enum Status { QUEUED, RUNNING, COMPLETED, FAILED }

    static ReviewJob queued(String id, String url) {
        return new ReviewJob(id, url, Status.QUEUED, Instant.now(), null, null, null);
    }

    ReviewJob running() {
        return new ReviewJob(id, url, Status.RUNNING, submittedAt, null, null, null);
    }

    ReviewJob completed(PullRequestReviewReport result) {
        return new ReviewJob(id, url, Status.COMPLETED, submittedAt, Instant.now(), result, null);
    }

    ReviewJob failed(String message) {
        return new ReviewJob(id, url, Status.FAILED, submittedAt, Instant.now(), null, message);
    }

    /** Whether the job reached a final state. */
    public boolean finished() {
        return status == Status.COMPLETED || status == Status.FAILED;
    }
}
