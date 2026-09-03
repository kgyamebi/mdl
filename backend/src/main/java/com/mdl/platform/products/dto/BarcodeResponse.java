package com.mdl.platform.products.dto;

public record BarcodeResponse(
        Long id,
        String barcode,
        String barcodeType,
        boolean primary
) {
}
