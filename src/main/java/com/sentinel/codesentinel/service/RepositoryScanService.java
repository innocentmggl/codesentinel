package com.sentinel.codesentinel.service;

import com.sentinel.codesentinel.domain.AnalysisResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.stream.Stream;

@Service
public class RepositoryScanService {

    private final AnalysisService analysisService;
    private final int maxConcurrency;

    public RepositoryScanService(AnalysisService analysisService,
                                 @Value("${sentinel.llm.max-concurrency:2}") int maxConcurrency) {
        this.analysisService = analysisService;
        this.maxConcurrency = maxConcurrency;
    }

    public List<AnalysisResult> analyzeRepository(Path root) throws IOException {
        List<Path> files;
        try (Stream<Path> walk = Files.walk(root)) {
            files = walk.filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".java"))
                    .toList();
        }

        // Virtual threads make fan-out cheap; the semaphore protects the LLM,
        // which can only serve a few requests at a time (especially locally).
        Semaphore llmPermits = new Semaphore(maxConcurrency);
        Map<Path, Future<AnalysisResult>> futures = new LinkedHashMap<>();

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (Path file : files) {
                futures.put(file, executor.submit(() -> {
                    llmPermits.acquire();
                    try {
                        return analysisService.analyzeFile(file);
                    } finally {
                        llmPermits.release();
                    }
                }));
            }

            List<AnalysisResult> results = new ArrayList<>();
            for (Map.Entry<Path, Future<AnalysisResult>> entry : futures.entrySet()) {
                try {
                    results.add(entry.getValue().get());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    results.add(AnalysisResult.failed(entry.getKey().toString(), "Interrupted"));
                } catch (ExecutionException e) {
                    results.add(AnalysisResult.failed(entry.getKey().toString(), e.getCause().getMessage()));
                }
            }
            return results;
        }
    }
}