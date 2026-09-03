package com.mdl.platform.copilot.dto;

import java.util.List;

public record CopilotChatResponse(
        Long conversationId,
        String reply,
        String provider,
        String model,
        int promptTokens,
        int completionTokens,
        List<String> suggestedFollowUps
) {
}
