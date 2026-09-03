package com.mdl.platform.users.dto;

public record LocationAssignmentResponse(
        Long locationId,
        String locationCode,
        String locationName,
        String locationType,
        String accessLevel
) {
}
