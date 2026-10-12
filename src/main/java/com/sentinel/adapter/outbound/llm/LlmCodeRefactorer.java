package com.sentinel.adapter.outbound.llm;

import java.util.List;
import java.util.Map;

import com.sentinel.application.port.CodeRefactorer;
import com.sentinel.domain.code.CodeChunk;
import com.sentinel.domain.review.Refactoring;
import org.springframework.stereotype.Component;

@Component
class LlmCodeRefactorer implements CodeRefactorer {

    private final StructuredChatClient chat;
    private final PromptLibrary prompts;

    LlmCodeRefactorer(StructuredChatClient chat, PromptLibrary prompts) {
        this.chat = chat;
        this.prompts = prompts;
    }

    @Override
    public Refactoring refactor(String code, String goal, List<CodeChunk> relatedCode) {
        return chat.ask(
                prompts.get("refactor-system"),
                prompts.get("refactor-user"),
                Map.of("context", RelatedCode.format(relatedCode), "goal", goal, "code", code),
                Refactoring.class);
    }
}
