package com.mdl.platform.authorization.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record GrantTemporaryPermissionRequest(
        @NotNull Long userId,
        @NotBlank @Size(max = 100) String permissionCode,
        @NotNull Long locationId,
        @Size(max = 32) String referenceType,
        Long referenceId,
        @Size(max = 500) String reason,
        @NotNull @Future Instant expiresAt
) {
}
