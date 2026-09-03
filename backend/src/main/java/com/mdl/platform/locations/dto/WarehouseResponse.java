package com.mdl.platform.locations.dto;

public record WarehouseResponse(
        Long id,
        String code,
        String name,
        String warehouseType,
        boolean restricted,
        String description,
        String status,
        LocationSummaryResponse location
) {
}
