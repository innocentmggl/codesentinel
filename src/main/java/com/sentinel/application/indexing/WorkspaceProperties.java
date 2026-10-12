package com.sentinel.application.indexing;

import java.nio.file.Path;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "sentinel.workspace")
public record WorkspaceProperties(@DefaultValue(".") Path root) {
}
