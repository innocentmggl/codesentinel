package com.sentinel.codesentinel.domain;

public record MethodMetric(String name,
                           int startLine,
                           int endLine,
                           int complexity) {}


