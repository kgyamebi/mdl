package com.mdl.platform.auth.dto;

public record MfaSetupResponse(
        String secret,
        String otpAuthUrl
) {
}
