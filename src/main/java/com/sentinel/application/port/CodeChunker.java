package com.sentinel.application.port;

import java.util.List;

import com.sentinel.domain.code.CodeChunk;
import com.sentinel.domain.code.SourceFile;

public interface CodeChunker {

    List<CodeChunk> chunk(SourceFile file);
}
