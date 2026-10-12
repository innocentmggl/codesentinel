package com.sentinel.application.pullrequest;

import java.util.List;

import com.sentinel.domain.scm.ReviewComment;

sealed interface FileOutcome {

    record Reviewed(List<ReviewComment> comments) implements FileOutcome {
    }

    record Skipped(String path, String reason) implements FileOutcome {
    }
}
