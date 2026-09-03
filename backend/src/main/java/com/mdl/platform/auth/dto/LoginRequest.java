package com.mdl.platform.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "Email or username is required")
        String login,

        @NotBlank(message = "Password is required")
        String password
) {
}
