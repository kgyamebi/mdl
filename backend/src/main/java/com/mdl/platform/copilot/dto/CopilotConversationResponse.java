package com.mdl.platform.copilot.dto;

import java.time.Instant;
import java.util.List;

public record CopilotConversationResponse(
        Long id,
        String title,
        Instant createdAt,
        Instant updatedAt,
        List<CopilotMessageResponse> messages
) {
}
