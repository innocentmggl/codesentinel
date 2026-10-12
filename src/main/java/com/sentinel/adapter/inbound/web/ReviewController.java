package com.sentinel.adapter.inbound.web;

import com.sentinel.application.refactor.RefactoringService;
import com.sentinel.application.review.SnippetReviewService;
import com.sentinel.domain.review.Refactoring;
import com.sentinel.domain.review.Review;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
class ReviewController {

    private final SnippetReviewService reviews;
    private final RefactoringService refactorings;

    ReviewController(SnippetReviewService reviews, RefactoringService refactorings) {
        this.reviews = reviews;
        this.refactorings = refactorings;
    }

    @PostMapping("/reviews")
    Review review(@Valid @RequestBody ReviewRequest request) {
        return reviews.review(request.code(), request.project());
    }

    @PostMapping("/refactorings")
    Refactoring refactor(@Valid @RequestBody RefactoringRequest request) {
        return refactorings.refactor(request.code(), request.goal(), request.project());
    }

    record ReviewRequest(@NotBlank String code, String project) {
    }

    record RefactoringRequest(@NotBlank String code, @NotBlank String goal, String project) {
    }
}
