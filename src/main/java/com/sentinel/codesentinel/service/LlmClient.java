package com.sentinel.codesentinel.service;

import com.sentinel.codesentinel.domain.AnalysisResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

@Component
public class LlmClient {

    private static final Logger log = LoggerFactory.getLogger(LlmClient.class);

    private final ChatClient chatClient;
    private final Resource userTemplate;

    public LlmClient(ChatClient chatClient,
                     @Value("classpath:/prompts/user-analyze.st") Resource userTemplate) {
        this.chatClient = chatClient;
        this.userTemplate = userTemplate;
    }

    // Retry is the outer wrapper, the circuit breaker the inner one.
    // Annotations only work when this bean is called from another bean (it is).
    @Retry(name = "llm", fallbackMethod = "fallback")
    @CircuitBreaker(name = "llm")
    public AnalysisResponse analyze(String className, String metrics, String rules, String code) {
        return chatClient.prompt()
                .user(u -> u.text(userTemplate)
                        .param("className", className)
                        .param("metrics", metrics)
                        .param("rules", rules)
                        .param("code", code))
                .call()
                .entity(AnalysisResponse.class);   // BeanOutputConverter under the hood
    }

    AnalysisResponse fallback(String className, String metrics, String rules, String code, Throwable t) {
        log.warn("LLM call failed for {}: {}", className, t.toString());
        return AnalysisResponse.unavailable("LLM unavailable or returned invalid JSON: " + t.getMessage());
    }
}