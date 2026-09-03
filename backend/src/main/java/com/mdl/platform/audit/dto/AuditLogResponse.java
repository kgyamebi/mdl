package com.mdl.platform.audit.dto;

import java.time.Instant;

public record AuditLogResponse(
        Long id,
        Long userId,
        String action,
        String module,
        String entityType,
        Long entityId,
        String entityRef,
        String summary,
        String details,
        String ipAddress,
        Instant createdAt
) {
}
