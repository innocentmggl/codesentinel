package com.sentinel.application.indexing;

import com.sentinel.application.port.CheckedOutRepository;
import com.sentinel.application.port.RepositoryFetcher;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class RepositoryIndexingService {

    private final RepositoryFetcher fetcher;
    private final ProjectIndexer indexer;

    RepositoryIndexingService(RepositoryFetcher fetcher, ProjectIndexer indexer) {
        this.fetcher = fetcher;
        this.indexer = indexer;
    }

    /**
     * @param repository host-specific reference, for example {@code owner/repo}
     * @param branch     branch or tag; null selects the default branch
     * @param project    logical project name; defaults to the repository name
     */
    public IndexingReport index(String repository, String branch, String project) {
        try (CheckedOutRepository checkout = fetcher.checkout(repository, branch)) {
            String name = StringUtils.hasText(project) ? project : checkout.name();
            return indexer.index(checkout.root(), name);
        }
    }
}
