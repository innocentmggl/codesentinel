package com.sentinel.adapter.outbound.vector;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import com.sentinel.domain.code.ChunkKind;
import com.sentinel.domain.code.CodeChunk;
import org.springframework.ai.document.Document;

final class ChunkDocuments {

    static final String PROJECT = "project";
    private static final String FILE_PATH = "filePath";
    private static final String OWNER = "owner";
    private static final String SIGNATURE = "signature";
    private static final String KIND = "kind";
    private static final String START_LINE = "startLine";
    private static final String END_LINE = "endLine";

    private ChunkDocuments() {
    }

    static Document toDocument(String project, CodeChunk chunk) {
        return Document.builder()
                .id(idOf(project, chunk))
                .text(chunk.code())
                .metadata(Map.of(
                        PROJECT, project,
                        FILE_PATH, chunk.filePath(),
                        OWNER, chunk.owner(),
                        SIGNATURE, chunk.signature(),
                        KIND, chunk.kind().name(),
                        START_LINE, chunk.startLine(),
                        END_LINE, chunk.endLine()))
                .build();
    }

    static CodeChunk toChunk(Document document) {
        Map<String, Object> metadata = document.getMetadata();
        return new CodeChunk(
                text(metadata, FILE_PATH, ""),
                text(metadata, OWNER, ""),
                text(metadata, SIGNATURE, ""),
                ChunkKind.valueOf(text(metadata, KIND, ChunkKind.METHOD.name())),
                number(metadata, START_LINE),
                number(metadata, END_LINE),
                Objects.requireNonNullElse(document.getText(), ""));
    }

    /** Deterministic id, so indexing the same code twice never creates duplicates. */
    private static String idOf(String project, CodeChunk chunk) {
        String key = "%s|%s|%s|%d".formatted(project, chunk.filePath(), chunk.signature(), chunk.startLine());
        return UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8)).toString();
    }

    private static String text(Map<String, Object> metadata, String key, String fallback) {
        Object value = metadata.get(key);
        return value == null ? fallback : value.toString();
    }

    private static int number(Map<String, Object> metadata, String key) {
        return metadata.get(key) instanceof Number number ? number.intValue() : 0;
    }
}
