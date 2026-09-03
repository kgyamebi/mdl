package com.mdl.platform.auth.dto;

import java.util.Set;

public record AuthUserResponse(
        Long id,
        String email,
        String username,
        String fullName,
        Long businessId,
        String businessCode,
        String businessName,
        String currencyCode,
        boolean mfaEnabled,
        Set<String> roles,
        Set<String> permissions
) {
}
