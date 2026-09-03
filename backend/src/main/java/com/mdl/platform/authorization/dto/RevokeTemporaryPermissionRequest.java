package com.mdl.platform.authorization.dto;

import jakarta.validation.constraints.Size;

public record RevokeTemporaryPermissionRequest(
        @Size(max = 500) String reason
) {
}
