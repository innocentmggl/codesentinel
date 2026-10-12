package com.sentinel.adapter.outbound.llm;

import java.util.List;
import java.util.stream.Collectors;

import com.sentinel.domain.code.CodeChunk;

final class RelatedCode {

    private static final String NONE = "No related project code available.";

    private RelatedCode() {
    }

    static String format(List<CodeChunk> chunks) {
        if (chunks.isEmpty()) {
            return NONE;
        }
        return chunks.stream()
                .map(chunk -> "// %s :: %s\n%s".formatted(chunk.filePath(), chunk.signature(), chunk.code()))
                .collect(Collectors.joining("\n\n"));
    }
}
