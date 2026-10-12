package com.sentinel.adapter.outbound.filesystem;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.sentinel.application.port.SourceFileLocator;
import com.sentinel.domain.code.SourceFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
class FileSystemSourceLocator implements SourceFileLocator {

    private static final Logger log = LoggerFactory.getLogger(FileSystemSourceLocator.class);

    private final SourceProperties properties;

    FileSystemSourceLocator(SourceProperties properties) {
        this.properties = properties;
    }

    @Override
    public List<SourceFile> findJavaFiles(Path root) {
        try (Stream<Path> paths = Files.walk(root)) {
            return paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".java"))
                    .filter(path -> !isExcluded(root.relativize(path)))
                    .sorted()
                    .flatMap(path -> read(root, path).stream())
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot scan " + root, e);
        }
    }

    private boolean isExcluded(Path relativePath) {
        return StreamSupport.stream(relativePath.spliterator(), false)
                .map(Path::toString)
                .anyMatch(properties.excludedDirs()::contains);
    }

    private Optional<SourceFile> read(Path root, Path file) {
        try {
            String relativePath = root.relativize(file).toString().replace('\\', '/');
            return Optional.of(new SourceFile(relativePath, Files.readString(file)));
        } catch (IOException e) {
            log.warn("Skipping unreadable file {}: {}", file, e.getMessage());
            return Optional.empty();
        }
    }
}
