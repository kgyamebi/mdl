package com.mdl.platform.users.dto;

import java.time.Instant;
import java.util.List;
import java.util.Set;

public record CreateUserResponse(
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
        Instant createdAt,
        String generatedPassword
) {
    public static CreateUserResponse from(UserResponse user, String generatedPassword) {
        return new CreateUserResponse(
                user.id(),
                user.email(),
                user.username(),
                user.firstName(),
                user.lastName(),
                user.fullName(),
                user.phone(),
                user.status(),
                user.roles(),
                user.locations(),
                user.lastLoginAt(),
                user.createdAt(),
                generatedPassword);
    }
}
