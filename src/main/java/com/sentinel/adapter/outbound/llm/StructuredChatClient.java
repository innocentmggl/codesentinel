package com.sentinel.adapter.outbound.llm;

import java.util.Map;

import com.sentinel.common.error.ExternalServiceException;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.resilience.annotation.ConcurrencyLimit;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Component;

@Component
public class StructuredChatClient {

    private final ChatClient chatClient;

    StructuredChatClient(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    @Retryable(maxRetries = 2, delay = 1000, multiplier = 2)
    @ConcurrencyLimit(limitString = "${sentinel.llm.max-concurrency:4}")
    public <T> T ask(String systemPrompt, String userTemplate, Map<String, Object> variables, Class<T> responseType) {
        T answer = chatClient.prompt()
                .system(systemPrompt)
                .user(user -> {
                    user.text(userTemplate);
                    variables.forEach(user::param);
                })
                .call()
                .entity(responseType);
        if (answer == null) {
            throw new ExternalServiceException("The model returned an empty response");
        }
        return answer;
    }
}
