package com.sentinel.domain.review;

import java.util.List;

public record Review(String summary, List<Finding> findings) {

    public Review {
        findings = findings == null ? List.of() : List.copyOf(findings);
    }
}
