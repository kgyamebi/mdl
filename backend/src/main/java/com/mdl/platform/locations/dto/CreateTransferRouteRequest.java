package com.mdl.platform.locations.dto;

import jakarta.validation.constraints.NotNull;

public record CreateTransferRouteRequest(
        @NotNull Long fromWarehouseId,
        @NotNull Long toWarehouseId,
        String notes
) {
}
