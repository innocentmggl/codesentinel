package com.sentinel.adapter.outbound.github;

import com.sentinel.common.error.ExternalServiceException;

public class GitHubApiException extends ExternalServiceException {

    private final int status;

    public GitHubApiException(int status, String message) {
        super("GitHub returned HTTP " + status + ": " + message);
        this.status = status;
    }

    public int status() {
        return status;
    }
}
