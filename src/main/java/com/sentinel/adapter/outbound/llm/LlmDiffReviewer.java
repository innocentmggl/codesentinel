package com.sentinel.adapter.outbound.llm;

import java.util.Map;

import com.sentinel.application.port.DiffReviewer;
import com.sentinel.common.Texts;
import com.sentinel.domain.review.DiffReviewRequest;
import com.sentinel.domain.review.Review;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
class LlmDiffReviewer implements DiffReviewer {

    private static final int MAX_DESCRIPTION_CHARS = 2000;

    private final StructuredChatClient chat;
    private final PromptLibrary prompts;

    LlmDiffReviewer(StructuredChatClient chat, PromptLibrary prompts) {
        this.chat = chat;
        this.prompts = prompts;
    }

    @Override
    public Review review(DiffReviewRequest request) {
        return chat.ask(
                prompts.get("diff-review-system"),
                prompts.get("diff-review-user"),
                Map.of(
                        "title", request.pullRequestTitle(),
                        "description", describe(request.pullRequestDescription()),
                        "path", request.filePath(),
                        "context", RelatedCode.format(request.relatedCode()),
                        "diff", request.numberedDiff(),
                        "maxFindings", String.valueOf(request.maxFindings())),
                Review.class);
    }

    private static String describe(String description) {
        return StringUtils.hasText(description) ? Texts.truncate(description, MAX_DESCRIPTION_CHARS) : "(none)";
    }
}
