package com.mdl.platform.sales.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public record CreateSaleReturnRequest(
        @NotBlank @Size(max = 32) String reason,
        @Size(max = 1000) String notes,
        @NotEmpty @Valid List<CreateSaleReturnItemRequest> items,
        @NotEmpty @Valid List<CreateSaleReturnRefundRequest> refunds
) {
    public record CreateSaleReturnItemRequest(
            @NotNull Long saleItemId,
            @NotNull @Positive BigDecimal quantity
    ) {
    }

    public record CreateSaleReturnRefundRequest(
            @NotBlank @Size(max = 32) String paymentMethod,
            @NotNull @Positive BigDecimal amount,
            @Size(max = 100) String reference
    ) {
    }
}
