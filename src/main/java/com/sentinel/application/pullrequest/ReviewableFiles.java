package com.sentinel.application.pullrequest;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

import com.sentinel.domain.scm.ChangeType;
import com.sentinel.domain.scm.ChangedFile;
import org.springframework.stereotype.Component;

@Component
class ReviewableFiles {

    private final PullRequestReviewProperties properties;

    ReviewableFiles(PullRequestReviewProperties properties) {
        this.properties = properties;
    }

    /** @return why the file is skipped, or empty when it should be reviewed */
    Optional<String> skipReason(ChangedFile file) {
        if (file.type() == ChangeType.REMOVED) {
            return Optional.of("file deleted");
        }
        if (file.patch() == null || file.patch().isBlank()) {
            return Optional.of("no textual diff (binary, renamed without changes, or too large for the platform)");
        }
        if (isIgnored(file.path())) {
            return Optional.of("ignored by configuration");
        }
        if (file.patch().length() > properties.maxPatchChars()) {
            return Optional.of("diff too large (%d characters, limit %d)"
                    .formatted(file.patch().length(), properties.maxPatchChars()));
        }
        return Optional.empty();
    }

    private boolean isIgnored(String path) {
        String[] segments = path.split("/");
        boolean inIgnoredDirectory = Arrays.stream(segments, 0, segments.length - 1)
                .anyMatch(properties.ignoredDirs()::contains);
        String fileName = segments[segments.length - 1].toLowerCase(Locale.ROOT);
        boolean ignoredSuffix = properties.ignoredFileSuffixes().stream()
                .anyMatch(suffix -> fileName.endsWith(suffix.toLowerCase(Locale.ROOT)));
        return inIgnoredDirectory || ignoredSuffix;
    }
}
