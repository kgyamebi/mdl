package com.mdl.platform.businesses.dto;

public record CurrencyResponse(
        String code,
        String name,
        String symbol,
        int decimalPlaces
) {
}
