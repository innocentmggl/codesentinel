package com.sentinel.application.indexing;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.sentinel.application.port.CodeChunker;
import com.sentinel.application.port.CodeIndex;
import com.sentinel.application.port.SourceFileLocator;
import com.sentinel.domain.code.ChunkKind;
import com.sentinel.domain.code.CodeChunk;
import com.sentinel.domain.code.SourceFile;
import org.junit.jupiter.api.Test;

class ProjectIndexerTest {

    @Test
    void replacesTheProjectIndexWithTheChunksOfEveryFile() {
        SourceFileLocator locator = _ -> List.of(new SourceFile("A.java", "a"), new SourceFile("B.java", "b"));
        CodeChunker chunker = file -> List.of(chunk(file.path()), chunk(file.path()));
        var index = new RecordingIndex();

        IndexingReport report = new ProjectIndexer(locator, chunker, index).index(Path.of("."), "shop");

        assertThat(report).isEqualTo(new IndexingReport("shop", 2, 4));
        assertThat(index.project).isEqualTo("shop");
        assertThat(index.chunks).hasSize(4);
    }

    @Test
    void anEmptyProjectClearsTheIndex() {
        var index = new RecordingIndex();

        new ProjectIndexer(_ -> List.of(), _ -> List.of(), index).index(Path.of("."), "shop");

        assertThat(index.project).isEqualTo("shop");
        assertThat(index.chunks).isEmpty();
    }

    private static CodeChunk chunk(String path) {
        return new CodeChunk(path, "Owner", "void run()", ChunkKind.METHOD, 1, 2, "code");
    }

    private static final class RecordingIndex implements CodeIndex {
        String project;
        List<CodeChunk> chunks = new ArrayList<>();

        @Override
        public void replaceProject(String project, List<CodeChunk> chunks) {
            this.project = project;
            this.chunks = chunks;
        }

        @Override
        public List<CodeChunk> findRelated(String query, String project) {
            return List.of();
        }
    }
}
