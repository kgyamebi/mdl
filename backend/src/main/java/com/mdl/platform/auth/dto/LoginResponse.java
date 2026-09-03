package com.mdl.platform.auth.dto;

public record LoginResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresInMinutes,
        AuthUserResponse user,
        Boolean mfaRequired,
        String mfaToken
) {
    public LoginResponse(
            String accessToken,
            String refreshToken,
            String tokenType,
            long expiresInMinutes,
            AuthUserResponse user) {
        this(accessToken, refreshToken, tokenType, expiresInMinutes, user, false, null);
    }

    public static LoginResponse mfaChallenge(String mfaToken) {
        return new LoginResponse(null, null, "Bearer", 0, null, true, mfaToken);
    }
}
