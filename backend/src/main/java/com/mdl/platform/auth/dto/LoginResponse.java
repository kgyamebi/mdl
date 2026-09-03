package com.mdl.platform.auth.dto;

public record LoginResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresInMinutes,
        AuthUserResponse user
) {
}
