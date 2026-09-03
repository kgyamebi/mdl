package com.mdl.platform.users.dto;

import java.time.Instant;
import java.util.List;
import java.util.Set;

public record UserResponse(
        Long id,
        String email,
        String username,
        String firstName,
        String lastName,
        String fullName,
        String phone,
        String status,
        Set<String> roles,
        List<LocationAssignmentResponse> locations,
        Instant lastLoginAt,
        Instant createdAt
) {
}
