package com.mdl.platform.auth.dto;

public record MfaConfirmRequest(
        String code
) {
}
