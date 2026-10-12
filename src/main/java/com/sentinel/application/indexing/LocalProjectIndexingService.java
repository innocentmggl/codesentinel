package com.sentinel.application.indexing;

import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;

import com.sentinel.common.error.InvalidRequestException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class LocalProjectIndexingService {

    private final ProjectIndexer indexer;
    private final Path workspaceRoot;

    LocalProjectIndexingService(ProjectIndexer indexer, WorkspaceProperties workspace) {
        this.indexer = indexer;
        this.workspaceRoot = workspace.root().toAbsolutePath().normalize();
    }

    /**
     * @param project logical project name; defaults to the directory name
     */
    public IndexingReport index(String rawPath, String project) {
        Path root = resolveInsideWorkspace(rawPath);
        return indexer.index(root, projectName(root, project));
    }

    private Path resolveInsideWorkspace(String rawPath) {
        Path root;
        try {
            root = Path.of(rawPath).toAbsolutePath().normalize();
        } catch (InvalidPathException e) {
            throw new InvalidRequestException("Invalid path: " + rawPath);
        }
        if (!root.startsWith(workspaceRoot)) {
            throw new InvalidRequestException("Path must be inside the workspace root: " + workspaceRoot);
        }
        if (!Files.isDirectory(root)) {
            throw new InvalidRequestException("Not a directory: " + root);
        }
        return root;
    }

    private static String projectName(Path root, String requested) {
        if (StringUtils.hasText(requested)) {
            return requested;
        }
        Path name = root.getFileName();
        if (name == null) {
            throw new InvalidRequestException("Cannot derive a project name from '" + root + "'; pass 'project'");
        }
        return name.toString();
    }
}
