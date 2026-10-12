package com.sentinel.application.port;

import java.nio.file.Path;
import java.util.List;

import com.sentinel.domain.code.SourceFile;

public interface SourceFileLocator {

    List<SourceFile> findJavaFiles(Path root);
}
