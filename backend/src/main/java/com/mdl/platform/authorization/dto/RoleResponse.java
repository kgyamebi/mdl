package com.mdl.platform.authorization.dto;

public record RoleResponse(
        Long id,
        String code,
        String name,
        String description,
        boolean system
) {
}
