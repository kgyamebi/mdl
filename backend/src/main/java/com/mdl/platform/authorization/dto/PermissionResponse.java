package com.mdl.platform.authorization.dto;

public record PermissionResponse(
        Long id,
        String code,
        String name,
        String module
) {
}
