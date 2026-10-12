package com.sentinel.domain.review;

import java.util.List;

public record Refactoring(String refactoredCode, List<String> changes) {

    public Refactoring {
        changes = changes == null ? List.of() : List.copyOf(changes);
    }
}
