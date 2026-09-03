package com.mdl.platform.businesses.dto;

import java.time.Instant;

public record BusinessResponse(
        Long id,
        String code,
        String name,
        String legalName,
        String currencyCode,
        String currencyName,
        String currencySymbol,
        String timezone,
        String status,
        Instant createdAt
) {
}
