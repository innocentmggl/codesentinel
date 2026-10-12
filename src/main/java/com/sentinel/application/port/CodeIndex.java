package com.sentinel.application.port;

import java.util.List;

import com.sentinel.domain.code.CodeChunk;

public interface CodeIndex {

    void replaceProject(String project, List<CodeChunk> chunks);

    List<CodeChunk> findRelated(String query, String project);
}
