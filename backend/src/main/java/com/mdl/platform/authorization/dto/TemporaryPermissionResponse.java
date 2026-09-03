package com.mdl.platform.authorization.dto;

import java.time.Instant;

public record TemporaryPermissionResponse(
        Long id,
        Long userId,
        String permissionCode,
        Long locationId,
        String locationCode,
        String locationName,
        String referenceType,
        Long referenceId,
        String reason,
        Long grantedBy,
        Instant expiresAt,
        String status,
        Instant revokedAt,
        Long revokedBy,
        String revokeReason,
        Instant createdAt
) {
}
