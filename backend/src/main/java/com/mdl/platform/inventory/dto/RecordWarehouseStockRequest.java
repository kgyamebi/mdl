package com.mdl.platform.inventory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record RecordWarehouseStockRequest(
        @NotNull Long locationId,
        @NotNull Long productId,
        @NotNull BigDecimal quantityChange,
        @NotBlank @Size(max = 500) String reason
) {
}
