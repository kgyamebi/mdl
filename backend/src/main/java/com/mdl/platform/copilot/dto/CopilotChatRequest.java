package com.mdl.platform.copilot.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CopilotChatRequest(
        @NotBlank @Size(max = 4000) String message,
        Long conversationId
) {
}
