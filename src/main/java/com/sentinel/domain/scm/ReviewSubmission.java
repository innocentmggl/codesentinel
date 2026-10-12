package com.sentinel.domain.scm;

import java.util.List;

public record ReviewSubmission(String body, List<ReviewComment> comments) {

    public ReviewSubmission {
        comments = List.copyOf(comments);
    }
}
