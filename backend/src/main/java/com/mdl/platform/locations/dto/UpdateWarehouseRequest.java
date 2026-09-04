package com.mdl.platform.locations.dto;

public record UpdateWarehouseRequest(
        String name,
        String city,
        String description,
        Boolean restricted
) {
}
