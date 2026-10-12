package com.sentinel.adapter.outbound.github;

import org.jspecify.annotations.NonNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "sentinel.github")
public record GitHubProperties(
        String token,
        @DefaultValue("https://api.github.com") String apiUrl,
        @DefaultValue("https://github.com") String cloneUrl) {

    public boolean hasToken() {
        return token != null && !token.isBlank();
    }

    @Override
    public @NonNull String toString() {
        return "GitHubProperties[token=" + (hasToken() ? "****" : "<none>") + ", apiUrl=" + apiUrl + "]";
    }
}
