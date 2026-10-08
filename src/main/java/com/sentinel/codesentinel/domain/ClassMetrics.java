package com.sentinel.codesentinel.domain;

import java.util.Comparator;
import java.util.List;

public record ClassMetrics(String className,
                           String packageName,
                           List<String> imports,
                           List<MethodMetric> methods) {

    public int maxComplexity() {
        return methods.stream().mapToInt(MethodMetric::complexity).max().orElse(0);
    }

    public String toPromptSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("package=%s, methods=%d, maxComplexity=%d%n"
                .formatted(packageName, methods.size(), maxComplexity()));
        methods.stream()
                .sorted(Comparator.comparingInt(MethodMetric::complexity).reversed())
                .limit(10)
                .forEach(m -> sb.append("- %s (complexity %d)%n".formatted(m.name(), m.complexity())));
        return sb.toString();
    }
}