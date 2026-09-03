package com.mdl.platform.locations.dto;

public record ShopResponse(
        Long id,
        String code,
        String name,
        String status,
        LocationSummaryResponse location,
        Long warehouseId,
        String warehouseCode,
        String warehouseName
) {
}
