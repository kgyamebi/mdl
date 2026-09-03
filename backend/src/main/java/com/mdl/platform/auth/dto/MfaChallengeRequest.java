package com.mdl.platform.auth.dto;

public record MfaChallengeRequest(
        String mfaToken,
        String code
) {
}
