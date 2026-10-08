package com.sentinel.codesentinel.service;

import com.sentinel.codesentinel.domain.AnalysisResponse;
import com.sentinel.codesentinel.domain.AnalysisResult;
import com.sentinel.codesentinel.domain.ClassMetrics;
import com.sentinel.codesentinel.parser.AstAnalyzer;
import com.sentinel.codesentinel.parser.ParsedSource;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AnalysisService {

    private final AstAnalyzer astAnalyzer;
    private final VectorStore vectorStore;
    private final LlmClient llmClient;

    public AnalysisService(AstAnalyzer astAnalyzer, VectorStore vectorStore, LlmClient llmClient) {
        this.astAnalyzer = astAnalyzer;
        this.vectorStore = vectorStore;
        this.llmClient = llmClient;
    }

    public AnalysisResult analyzeFile(Path path) throws IOException {
        return analyzeSource(path.getFileName().toString(), Files.readString(path));
    }

    public AnalysisResult analyzeSource(String fileName, String source) {
        try {
            ParsedSource parsed = astAnalyzer.parse(source);
            ClassMetrics metrics = parsed.metrics();

            AnalysisResponse response = llmClient.analyze(
                    metrics.className(),
                    metrics.toPromptSummary(),
                    fetchRules(metrics),
                    parsed.condensedSource());

            return new AnalysisResult(fileName, metrics, parsed.reductionPercent(), response, null);
        } catch (Exception e) {
            return AnalysisResult.failed(fileName, e.getMessage());
        }
    }

    /** Retrieves the most relevant coding rules using the class name and its imports as the query. */
    private String fetchRules(ClassMetrics metrics) {
        String query = (metrics.className() + " " + String.join(" ", metrics.imports())).strip();
        if (query.length() > 800) {
            query = query.substring(0, 800);
        }
        List<Document> rules = vectorStore.similaritySearch(
                SearchRequest.builder().query(query).topK(3).build());

        if (rules.isEmpty()) {
            return "No custom rules have been ingested. Apply general Java best practices.";
        }
        return rules.stream().map(Document::getText).collect(Collectors.joining("\n---\n"));
    }
}