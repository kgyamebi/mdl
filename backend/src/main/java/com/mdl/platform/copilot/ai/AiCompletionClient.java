package com.mdl.platform.copilot.ai;

import com.mdl.platform.security.UserContext;

import java.util.List;

public interface AiCompletionClient {

    AiCompletionResult complete(AiCompletionRequest request);

    record ChatTurn(String role, String content) {
    }

    record AiCompletionRequest(
            UserContext user,
            String userMessage,
            String groundedContext,
            List<ChatTurn> history
    ) {
    }

    record AiCompletionResult(
            String reply,
            int promptTokens,
            int completionTokens,
            String model,
            String provider
    ) {
    }
}
