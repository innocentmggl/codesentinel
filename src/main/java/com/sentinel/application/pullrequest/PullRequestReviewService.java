package com.sentinel.application.pullrequest;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.sentinel.application.port.PullRequestHost;
import com.sentinel.common.error.InvalidRequestException;
import com.sentinel.domain.scm.ChangedFile;
import com.sentinel.domain.scm.PullRequest;
import com.sentinel.domain.scm.ReviewComment;
import com.sentinel.domain.scm.ReviewSubmission;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class PullRequestReviewService {

    private static final Logger log = LoggerFactory.getLogger(PullRequestReviewService.class);
    private static final Comparator<ReviewComment> MOST_SEVERE_FIRST =
            Comparator.comparing(ReviewComment::severity).reversed();
    private static final Comparator<ReviewComment> BY_LOCATION =
            Comparator.comparing(ReviewComment::path).thenComparingInt(ReviewComment::line);

    private final List<PullRequestHost> hosts;
    private final ReviewableFiles reviewableFiles;
    private final FileReviewer fileReviewer;
    private final CommentFormatter formatter;
    private final PullRequestReviewProperties properties;

    PullRequestReviewService(List<PullRequestHost> hosts,
                             ReviewableFiles reviewableFiles,
                             FileReviewer fileReviewer,
                             CommentFormatter formatter,
                             PullRequestReviewProperties properties) {
        this.hosts = hosts;
        this.reviewableFiles = reviewableFiles;
        this.fileReviewer = fileReviewer;
        this.formatter = formatter;
        this.properties = properties;
    }

    /**
     * @param project indexed project to take context from; defaults to the repository name
     * @param dryRun  when true the review is computed and returned but not posted
     */
    public PullRequestReviewReport review(String url, String project, boolean dryRun) {
        PullRequestHost host = hostFor(url);
        PullRequest pullRequest = host.fetch(url);
        String projectName = StringUtils.hasText(project) ? project : pullRequest.ref().repo();

        Selection selection = select(pullRequest.files());
        List<FileOutcome> outcomes = reviewConcurrently(pullRequest, projectName, selection.files());

        var skipped = new ArrayList<>(selection.skipped());
        var found = new ArrayList<ReviewComment>();
        int reviewed = 0;
        for (FileOutcome outcome : outcomes) {
            switch (outcome) {
                case FileOutcome.Reviewed result -> {
                    reviewed++;
                    found.addAll(result.comments());
                }
                case FileOutcome.Skipped result -> skipped.add(result.path() + ": " + result.reason());
            }
        }

        List<ReviewComment> comments = strongest(found);
        boolean post = !dryRun && reviewed > 0 && (!comments.isEmpty() || properties.postCleanReviews());
        String reviewUrl = null;
        if (post) {
            String body = formatter.summary(comments, reviewed, skipped);
            reviewUrl = host.submit(pullRequest, new ReviewSubmission(body, comments));
        }

        log.info("Reviewed {} ({} files reviewed, {} skipped, {} comments, posted={})",
                url, reviewed, skipped.size(), comments.size(), post);
        return new PullRequestReviewReport(url, post, reviewUrl, reviewed, List.copyOf(skipped), comments);
    }

    private PullRequestHost hostFor(String url) {
        return hosts.stream()
                .filter(host -> host.supports(url))
                .findFirst()
                .orElseThrow(() -> new InvalidRequestException("Unsupported pull request URL: " + url));
    }

    /** Splits the changed files into those worth reviewing (up to the limit) and the skipped ones with reasons. */
    private Selection select(List<ChangedFile> files) {
        var candidates = new ArrayList<ChangedFile>();
        var skipped = new ArrayList<String>();
        for (ChangedFile file : files) {
            reviewableFiles.skipReason(file).ifPresentOrElse(
                    reason -> skipped.add(file.path() + ": " + reason),
                    () -> candidates.add(file));
        }

        int limit = Math.min(properties.maxFiles(), candidates.size());
        candidates.subList(limit, candidates.size()).forEach(file ->
                skipped.add(file.path() + ": over the limit of " + properties.maxFiles() + " files per review"));
        return new Selection(List.copyOf(candidates.subList(0, limit)), skipped);
    }

    /** One virtual thread per file; the model adapter's concurrency limit keeps the provider from being flooded. */
    private List<FileOutcome> reviewConcurrently(PullRequest pullRequest, String project, List<ChangedFile> files) {
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<FileOutcome>> futures = files.stream()
                    .map(file -> executor.submit(() -> fileReviewer.review(pullRequest, file, project)))
                    .toList();
            return futures.stream().map(PullRequestReviewService::await).toList();
        }
    }

    /** The most severe comments up to the configured maximum, ordered by location for reading. */
    private List<ReviewComment> strongest(List<ReviewComment> comments) {
        return comments.stream()
                .sorted(MOST_SEVERE_FIRST)
                .limit(properties.maxComments())
                .sorted(BY_LOCATION)
                .toList();
    }

    private static FileOutcome await(Future<FileOutcome> future) {
        try {
            return future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Review was interrupted", e);
        } catch (ExecutionException e) {
            throw new IllegalStateException(e.getCause());
        }
    }

    private record Selection(List<ChangedFile> files, List<String> skipped) {
    }
}
