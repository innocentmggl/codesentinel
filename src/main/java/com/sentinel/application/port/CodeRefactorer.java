package com.sentinel.application.port;

import java.util.List;

import com.sentinel.domain.code.CodeChunk;
import com.sentinel.domain.review.Refactoring;

public interface CodeRefactorer {

    Refactoring refactor(String code, String goal, List<CodeChunk> relatedCode);
}
