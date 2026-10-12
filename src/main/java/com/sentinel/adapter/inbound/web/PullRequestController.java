package com.sentinel.adapter.inbound.web;

import java.net.URI;

import com.sentinel.application.pullrequest.PullRequestReviewJobs;
import com.sentinel.application.pullrequest.ReviewJob;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/** Reviews a pull request in the background and posts the result back to the hosting platform. */
@RestController
@RequestMapping("/api/v1/pull-requests")
class PullRequestController {

    private final PullRequestReviewJobs jobs;

    PullRequestController(PullRequestReviewJobs jobs) {
        this.jobs = jobs;
    }

    /** Accepts the review and returns at once; poll the {@code Location} header for the outcome. */
    @PostMapping("/reviews")
    ResponseEntity<ReviewJob> submit(@Valid @RequestBody PullRequestReviewRequest request) {
        ReviewJob job = jobs.submit(request.url(), request.project(), request.dryRun());
        URI status = ServletUriComponentsBuilder.fromCurrentRequestUri()
                .path("/{id}")
                .buildAndExpand(job.id())
                .toUri();
        return ResponseEntity.accepted().location(status).body(job);
    }

    @GetMapping("/reviews/{id}")
    ResponseEntity<ReviewJob> status(@PathVariable String id) {
        return jobs.find(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * @param project index to take context from; defaults to the repository name
     * @param dryRun  compute the review without posting it
     */
    record PullRequestReviewRequest(@NotBlank String url, String project, boolean dryRun) {
    }
}