package com.mdl.platform.sales.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record SalePaymentResponse(
        Long id,
        String paymentMethod,
        BigDecimal amount,
        String reference,
        Long receivedBy,
        Instant createdAt
) {
}
