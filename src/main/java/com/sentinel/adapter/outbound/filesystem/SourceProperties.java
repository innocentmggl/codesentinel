package com.sentinel.adapter.outbound.filesystem;

import java.util.Set;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "sentinel.source")
public record SourceProperties(
        @DefaultValue({"target", "build", ".git", ".idea", "node_modules"}) Set<String> excludedDirs) {
}
