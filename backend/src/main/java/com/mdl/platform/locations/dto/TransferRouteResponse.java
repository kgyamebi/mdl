package com.mdl.platform.locations.dto;

public record TransferRouteResponse(
        Long id,
        Long fromWarehouseId,
        String fromWarehouseCode,
        String fromWarehouseName,
        Long toWarehouseId,
        String toWarehouseCode,
        String toWarehouseName,
        boolean enabled,
        String notes
) {
}
