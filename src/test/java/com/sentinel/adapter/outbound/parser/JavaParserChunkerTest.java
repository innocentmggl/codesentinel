package com.sentinel.adapter.outbound.parser;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import com.sentinel.domain.code.ChunkKind;
import com.sentinel.domain.code.CodeChunk;
import com.sentinel.domain.code.SourceFile;
import org.junit.jupiter.api.Test;

class JavaParserChunkerTest {

    private final JavaParserChunker chunker = new JavaParserChunker(new ChunkingProperties(6000));

    @Test
    void producesOneChunkPerMethodAndConstructor() {
        String source = """
                package demo;

                public class Greeter {
                    private final String name;

                    public Greeter(String name) {
                        this.name = name;
                    }

                    public String greet() {
                        return "Hello " + name;
                    }

                    abstract static class Base {
                        abstract void declaredOnly();
                    }
                }
                """;

        List<CodeChunk> chunks = chunker.chunk(new SourceFile("demo/Greeter.java", source));

        assertThat(chunks).extracting(CodeChunk::kind)
                .containsExactlyInAnyOrder(ChunkKind.CONSTRUCTOR, ChunkKind.METHOD);
        assertThat(chunks).extracting(CodeChunk::owner).containsOnly("Greeter");
        assertThat(chunks).extracting(CodeChunk::signature)
                .containsExactlyInAnyOrder("Greeter(String name)", "String greet()");
    }

    @Test
    void recordsLinesAndFilePath() {
        String source = "class A {\n    void run() {\n    }\n}\n";

        CodeChunk chunk = chunker.chunk(new SourceFile("A.java", source)).getFirst();

        assertThat(chunk.filePath()).isEqualTo("A.java");
        assertThat(chunk.startLine()).isEqualTo(2);
        assertThat(chunk.endLine()).isEqualTo(3);
    }

    @Test
    void truncatesOversizedChunks() {
        var small = new JavaParserChunker(new ChunkingProperties(20));
        String source = "class A { void run() { System.out.println(\"a long statement\"); } }";

        assertThat(small.chunk(new SourceFile("A.java", source)).getFirst().code()).hasSize(20);
    }

    @Test
    void unparsableSourceYieldsNoChunks() {
        assertThat(chunker.chunk(new SourceFile("Broken.java", "class { void ("))).isEmpty();
    }
}
