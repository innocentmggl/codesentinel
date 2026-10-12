package com.sentinel.domain.code;

public record CodeChunk(
        String filePath,
        String owner,
        String signature,
        ChunkKind kind,
        int startLine,
        int endLine,
        String code) {
}
