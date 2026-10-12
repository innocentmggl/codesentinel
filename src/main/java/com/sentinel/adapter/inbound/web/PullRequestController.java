package com.sentinel.adapter.inbound.web;

import com.sentinel.application.pullrequest.PullRequestReviewReport;
import com.sentinel.application.pullrequest.PullRequestReviewService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/pull-requests")
class PullRequestController {

    private final PullRequestReviewService service;

    PullRequestController(PullRequestReviewService service) {
        this.service = service;
    }

    @PostMapping("/reviews")
    PullRequestReviewReport review(@Valid @RequestBody PullRequestReviewRequest request) {
        return service.review(request.url(), request.project(), request.dryRun());
    }

    /**
     * @param project index to take context from; defaults to the repository name
     * @param dryRun  compute the review without posting it
     */
    record PullRequestReviewRequest(@NotBlank String url, String project, boolean dryRun) {
    }
}
