package com.mdl.platform.locations.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateMainWarehouseRequest(
        @NotBlank String name,
        String code,
        String city,
        String country,
        String description,
        Boolean restricted,
        String warehouseType
) {
}
