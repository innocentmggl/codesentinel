package com.sentinel.adapter.outbound.llm;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Component
class PromptLibrary {

    private final Map<String, String> cache = new ConcurrentHashMap<>();

    String get(String name) {
        return cache.computeIfAbsent(name, PromptLibrary::load);
    }

    private static String load(String name) {
        try {
            return new ClassPathResource("prompts/" + name + ".txt").getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot load prompt '" + name + "'", e);
        }
    }
}
