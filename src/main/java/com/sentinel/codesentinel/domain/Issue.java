package com.sentinel.codesentinel.domain;

public record Issue(String severity,      // CRITICAL, HIGH, MEDIUM or LOW
                    String category,      // SECURITY, CORRECTNESS, MAINTAINABILITY, ...
                    String location,      // method or class name
                    String description,
                    String suggestion) {}