package com.sentinel.application.pullrequest;

import java.util.Set;

import com.sentinel.domain.review.Severity;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Limits and filters for pull request reviews.
 *
 * @param maxFiles            files reviewed per pull request; the rest are reported as skipped
 * @param maxPatchChars       files with a larger diff are skipped
 * @param maxCommentsPerFile  findings kept per file
 * @param maxComments         comments in the posted review, most severe first
 * @param minSeverity         findings below this severity are dropped
 * @param postCleanReviews    post a summary review even when no findings were kept
 * @param ignoredDirs         files below a directory with one of these names are skipped
 * @param ignoredFileSuffixes files whose name ends with one of these are skipped
 */
@ConfigurationProperties(prefix = "sentinel.pull-request")
public record PullRequestReviewProperties(
        @DefaultValue("40") int maxFiles,
        @DefaultValue("20000") int maxPatchChars,
        @DefaultValue("5") int maxCommentsPerFile,
        @DefaultValue("25") int maxComments,
        @DefaultValue("MINOR") Severity minSeverity,
        @DefaultValue("true") boolean postCleanReviews,
        @DefaultValue({"node_modules", "dist", "build", "target", "vendor", "generated", ".idea"})
        Set<String> ignoredDirs,
        @DefaultValue({".lock", "package-lock.json", ".min.js", ".map", ".svg", ".png", ".jpg", ".jpeg", ".gif",
                ".pdf", ".jar", ".zip"})
        Set<String> ignoredFileSuffixes) {
}
