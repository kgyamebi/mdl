package com.mdl.platform.products.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateProductRequest(
        Long categoryId,
        @NotBlank @Size(max = 64) String sku,
        @NotBlank @Size(max = 255) String name,
        String description,
        @Size(max = 100) String brand,
        @NotBlank @Pattern(regexp = "PIECE|METRE|BOX|ROLL|PACK|SET") String unitOfMeasure,
        @DecimalMin("0") BigDecimal costPrice,
        @NotNull @DecimalMin("0") BigDecimal sellingPrice,
        Boolean taxInclusive,
        Boolean trackInventory,
        Integer reorderLevel,
        @Size(max = 32) String status
) {
}
