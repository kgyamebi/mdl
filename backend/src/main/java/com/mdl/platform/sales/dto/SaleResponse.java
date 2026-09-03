package com.mdl.platform.sales.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record SaleResponse(
        Long id,
        String saleNumber,
        Long shopId,
        String shopCode,
        String shopName,
        Long shopLocationId,
        Long warehouseLocationId,
        String currencyCode,
        String status,
        BigDecimal subtotal,
        BigDecimal totalAmount,
        BigDecimal returnedAmount,
        String customerName,
        String notes,
        Long soldBy,
        Long cancelledBy,
        Instant cancelledAt,
        String cancelReason,
        Long refundedBy,
        Instant refundedAt,
        String refundReason,
        List<SaleItemResponse> items,
        List<SalePaymentResponse> payments,
        Instant createdAt,
        Instant updatedAt
) {
}
