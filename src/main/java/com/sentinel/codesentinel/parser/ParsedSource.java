package com.sentinel.codesentinel.parser;

import com.sentinel.codesentinel.domain.ClassMetrics;

public record ParsedSource(ClassMetrics metrics, String condensedSource,
                           int originalChars, int condensedChars) {

    public double reductionPercent() {
        return originalChars == 0 ? 0 : 100.0 * (originalChars - condensedChars) / originalChars;
    }
}