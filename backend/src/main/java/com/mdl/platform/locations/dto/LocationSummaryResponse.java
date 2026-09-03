package com.mdl.platform.locations.dto;

public record LocationSummaryResponse(
        Long id,
        String code,
        String name,
        String locationType,
        String city,
        String country,
        String status
) {
}
