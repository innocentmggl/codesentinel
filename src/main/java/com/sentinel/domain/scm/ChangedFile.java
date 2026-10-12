package com.sentinel.domain.scm;

/**
 * One file touched by a pull request.
 *
 * @param patch unified diff hunks for the file; null for binary or very large files
 */
public record ChangedFile(String path, ChangeType type, String patch) {
}
