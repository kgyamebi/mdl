package com.mdl.platform.sales.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public record CreateSaleRequest(
        @NotNull Long shopId,
        @Size(max = 255) String customerName,
        @Size(max = 1000) String notes,
        @NotEmpty @Valid List<CreateSaleItemRequest> items,
        @NotEmpty @Valid List<CreateSalePaymentRequest> payments
) {
    public record CreateSaleItemRequest(
            @NotNull Long productId,
            @NotNull @Positive BigDecimal quantity,
            @Positive BigDecimal unitPrice
    ) {
    }

    public record CreateSalePaymentRequest(
            @NotBlank @Pattern(regexp = "CASH|MOBILE_MONEY|CARD|BANK_TRANSFER") String paymentMethod,
            @NotNull @Positive BigDecimal amount,
            @Size(max = 100) String reference
    ) {
    }
}
