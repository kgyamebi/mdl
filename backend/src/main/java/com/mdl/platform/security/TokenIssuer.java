package com.mdl.platform.security;

import java.util.Set;

public interface TokenIssuer {

    String generateAccessToken(
            Long userId,
            String email,
            String username,
            Long businessId,
            String businessCode,
            String currencyCode,
            Set<String> roles,
            Set<String> permissions,
            Long sessionId);

    String generateRefreshToken();

    long getRefreshTokenExpiryDays();
}
