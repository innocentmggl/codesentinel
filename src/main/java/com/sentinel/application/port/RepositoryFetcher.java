package com.sentinel.application.port;

public interface RepositoryFetcher {

    /**
     * @param repository host-specific reference, for example {@code owner/repo}
     * @param branch     branch or tag; null selects the default branch
     */
    CheckedOutRepository checkout(String repository, String branch);
}
