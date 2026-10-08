package com.sentinel.codesentinel.domain;

public record AnalysisResult(String fileName, ClassMetrics metrics,
                             double sourceReductionPercent,
                             AnalysisResponse response, String error) {

    public static AnalysisResult failed(String fileName, String error) {
        return new AnalysisResult(fileName, null, 0, null, error);
    }
}