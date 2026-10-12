package com.sentinel.adapter.outbound.parser;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "sentinel.parser")
public record ChunkingProperties(@DefaultValue("6000") int maxChunkChars) {
}
