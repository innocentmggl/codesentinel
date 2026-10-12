package com.sentinel.application.pullrequest;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.UnaryOperator;

import com.sentinel.common.Texts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class PullRequestReviewJobs implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(PullRequestReviewJobs.class);
    private static final int MAX_ERROR_CHARS = 500;

    private final PullRequestReviewService reviews;
    private final ReviewJobProperties properties;
    private final Map<String, ReviewJob> jobs = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    PullRequestReviewJobs(PullRequestReviewService reviews, ReviewJobProperties properties) {
        this.reviews = reviews;
        this.properties = properties;
    }

    /**
     * Starts a review, or returns the active one for the same pull request.
     *
     * @throws com.sentinel.common.error.InvalidRequestException if no host supports the URL
     */
    public synchronized ReviewJob submit(String url, String project, boolean dryRun) {
        reviews.ensureSupported(url);
        evictExpired();

        Optional<ReviewJob> active = jobs.values().stream()
                .filter(job -> !job.finished() && job.url().equals(url))
                .findFirst();
        if (active.isPresent()) {
            return active.get();
        }

        ReviewJob job = ReviewJob.queued(UUID.randomUUID().toString(), url);
        jobs.put(job.id(), job);
        executor.submit(() -> run(job.id(), url, project, dryRun));
        return job;
    }

    public Optional<ReviewJob> find(String id) {
        return Optional.ofNullable(jobs.get(id));
    }

    private void run(String id, String url, String project, boolean dryRun) {
        update(id, ReviewJob::running);
        try {
            PullRequestReviewReport report = reviews.review(url, project, dryRun);
            update(id, job -> job.completed(report));
        } catch (RuntimeException e) {
            log.warn("Review of {} failed: {}", url, e.toString());
            String message = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            update(id, job -> job.failed(Texts.truncate(message, MAX_ERROR_CHARS)));
        }
    }

    private void update(String id, UnaryOperator<ReviewJob> change) {
        jobs.computeIfPresent(id, (key, job) -> change.apply(job));
    }

    private void evictExpired() {
        Instant cutoff = Instant.now().minus(properties.retention());
        jobs.values().removeIf(job -> job.finished() && job.finishedAt().isBefore(cutoff));
    }

    /** Called by Spring on shutdown: stops accepting work and lets running reviews finish. */
    @Override
    public void close() {
        executor.shutdown();
    }
}