package com.mdl.platform.users.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record AssignLocationsRequest(
        @NotEmpty List<@NotNull LocationAssignmentItem> locations
) {
    public record LocationAssignmentItem(
            @NotNull Long locationId,
            String accessLevel
    ) {
    }
}
