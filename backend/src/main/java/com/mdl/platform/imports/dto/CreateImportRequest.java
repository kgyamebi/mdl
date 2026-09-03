package com.mdl.platform.imports.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record CreateImportRequest(
        @NotBlank @Size(max = 255) String supplierName,
        @Size(max = 100) String supplierReference,
        @NotNull Long destinationLocationId,
        LocalDate expectedArrivalDate,
        @Size(max = 1000) String notes,
        Long assignedReceiverUserId,
        @NotEmpty @Valid List<CreateImportItemRequest> items
) {
    public record CreateImportItemRequest(
            @NotNull Long productId,
            @NotNull @DecimalMin("0.0001") BigDecimal expectedQuantity,
            @DecimalMin("0") BigDecimal unitCost,
            @Size(max = 500) String notes
    ) {
    }
}
