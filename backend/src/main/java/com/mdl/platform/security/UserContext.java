package com.mdl.platform.security;

import java.util.Set;

/**
 * Snapshot of the authenticated user for the current request.
 * Loaded from JWT and used by services for authorization checks.
 */
public record UserContext(
        Long userId,
        String email,
        String username,
        Long businessId,
        String businessCode,
        String currencyCode,
        Set<String> roles,
        Set<String> permissions,
        Long sessionId
) {
}
