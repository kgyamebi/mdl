package com.mdl.platform.copilot.dto;

import java.time.Instant;

public record CopilotMessageResponse(
        Long id,
        String role,
        String content,
        Instant createdAt
) {
}
