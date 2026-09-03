package com.mdl.platform.inventory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateDamageReportRequest(
        @NotNull Long locationId,
        @NotNull Long productId,
        @NotNull @Positive BigDecimal quantity,
        @NotBlank @Size(max = 500) String reason
) {
}
