package com.mdl.platform.users.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record CreateUserRequest(
        String email,
        String username,
        String password,
        @NotBlank String firstName,
        @NotBlank String lastName,
        String phone,
        List<@NotBlank String> roleCodes,
        List<Long> locationIds
) {
}
