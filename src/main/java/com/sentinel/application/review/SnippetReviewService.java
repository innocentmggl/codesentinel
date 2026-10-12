package com.sentinel.application.review;

import java.util.List;

import com.sentinel.application.port.CodeIndex;
import com.sentinel.application.port.SnippetReviewer;
import com.sentinel.domain.code.CodeChunk;
import com.sentinel.domain.review.Review;
import org.springframework.stereotype.Service;

@Service
public class SnippetReviewService {

    private final CodeIndex index;
    private final SnippetReviewer reviewer;

    SnippetReviewService(CodeIndex index, SnippetReviewer reviewer) {
        this.index = index;
        this.reviewer = reviewer;
    }

    /**
     * @param project indexed project to take context from; null or blank searches all projects
     */
    public Review review(String code, String project) {
        List<CodeChunk> related = index.findRelated(code, project);
        return reviewer.review(code, related);
    }
}
