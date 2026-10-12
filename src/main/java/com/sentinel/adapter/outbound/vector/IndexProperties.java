package com.sentinel.adapter.outbound.vector;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "sentinel.index")
public record IndexProperties(
        @DefaultValue("32") int batchSize,
        @DefaultValue("5") int topK,
        @DefaultValue("0.3") double similarityThreshold,
        @DefaultValue("4000") int maxQueryChars) {
}
