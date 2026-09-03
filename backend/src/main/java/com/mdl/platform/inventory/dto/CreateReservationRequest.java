package com.mdl.platform.inventory.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateReservationRequest(
        @NotNull Long locationId,
        @NotNull Long productId,
        @NotNull @Positive BigDecimal quantity,
        @Size(max = 32) String referenceType,
        Long referenceId,
        @Size(max = 500) String notes
) {
}
