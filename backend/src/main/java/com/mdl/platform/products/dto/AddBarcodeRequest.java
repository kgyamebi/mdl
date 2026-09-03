package com.mdl.platform.products.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AddBarcodeRequest(
        @NotBlank @Size(max = 64) String barcode,
        @NotBlank @Pattern(regexp = "EAN13|UPC|CODE128|INTERNAL|QR") String barcodeType,
        Boolean primary
) {
}
