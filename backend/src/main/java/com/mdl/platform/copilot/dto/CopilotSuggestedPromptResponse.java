package com.mdl.platform.copilot.dto;

public record CopilotSuggestedPromptResponse(
        String prompt,
        String category
) {
}
