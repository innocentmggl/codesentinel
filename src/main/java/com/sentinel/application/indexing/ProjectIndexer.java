package com.sentinel.application.indexing;

import java.nio.file.Path;
import java.util.List;

import com.sentinel.application.port.CodeChunker;
import com.sentinel.application.port.CodeIndex;
import com.sentinel.application.port.SourceFileLocator;
import com.sentinel.domain.code.CodeChunk;
import com.sentinel.domain.code.SourceFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ProjectIndexer {

    private static final Logger log = LoggerFactory.getLogger(ProjectIndexer.class);

    private final SourceFileLocator locator;
    private final CodeChunker chunker;
    private final CodeIndex index;

    ProjectIndexer(SourceFileLocator locator, CodeChunker chunker, CodeIndex index) {
        this.locator = locator;
        this.chunker = chunker;
        this.index = index;
    }

    public IndexingReport index(Path root, String project) {
        List<SourceFile> sources = locator.findJavaFiles(root);
        List<CodeChunk> chunks = sources.stream()
                .flatMap(source -> chunker.chunk(source).stream())
                .toList();

        index.replaceProject(project, chunks);

        log.info("Indexed project '{}': {} files, {} chunks", project, sources.size(), chunks.size());
        return new IndexingReport(project, sources.size(), chunks.size());
    }
}
