package com.sentinel.codesentinel.domain;

import java.util.List;

public record AnalysisResponse(String summary,
                               List<Issue> issues,
                               String refactoredCode,
                               String junitTests) {

    public static AnalysisResponse unavailable(String reason) {
        return new AnalysisResponse(reason, List.of(), null, null);
    }
}