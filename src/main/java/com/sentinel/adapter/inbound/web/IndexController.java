package com.sentinel.adapter.inbound.web;

import com.sentinel.application.indexing.IndexingReport;
import com.sentinel.application.indexing.LocalProjectIndexingService;
import com.sentinel.application.indexing.RepositoryIndexingService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
class IndexController {

    private final LocalProjectIndexingService localIndexing;
    private final RepositoryIndexingService repositoryIndexing;

    IndexController(LocalProjectIndexingService localIndexing, RepositoryIndexingService repositoryIndexing) {
        this.localIndexing = localIndexing;
        this.repositoryIndexing = repositoryIndexing;
    }

    @PostMapping("/index")
    IndexingReport indexDirectory(@Valid @RequestBody DirectoryRequest request) {
        return localIndexing.index(request.path(), request.project());
    }

    @PostMapping("/repositories/index")
    IndexingReport indexRepository(@Valid @RequestBody RepositoryRequest request) {
        return repositoryIndexing.index(request.repository(), request.branch(), request.project());
    }

    record DirectoryRequest(@NotBlank String path, String project) {
    }

    record RepositoryRequest(@NotBlank String repository, String branch, String project) {
    }
}
