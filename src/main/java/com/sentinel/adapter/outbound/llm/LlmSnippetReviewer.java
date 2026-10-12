package com.sentinel.adapter.outbound.llm;

import java.util.List;
import java.util.Map;

import com.sentinel.application.port.SnippetReviewer;
import com.sentinel.domain.code.CodeChunk;
import com.sentinel.domain.review.Review;
import org.springframework.stereotype.Component;

@Component
class LlmSnippetReviewer implements SnippetReviewer {

    private final StructuredChatClient chat;
    private final PromptLibrary prompts;

    LlmSnippetReviewer(StructuredChatClient chat, PromptLibrary prompts) {
        this.chat = chat;
        this.prompts = prompts;
    }

    @Override
    public Review review(String code, List<CodeChunk> relatedCode) {
        return chat.ask(
                prompts.get("review-system"),
                prompts.get("review-user"),
                Map.of("context", RelatedCode.format(relatedCode), "code", code),
                Review.class);
    }
}
