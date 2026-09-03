package com.mdl.platform.locations.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateTransferRouteRequest(
        @NotNull Boolean enabled,
        String notes
) {
}
