package com.mdl.platform.reports.dto;

import java.math.BigDecimal;
import java.util.List;

public record InventoryValuationReport(
        String currencyCode,
        BigDecimal totalCostValue,
        BigDecimal totalRetailValue,
        long balanceLineCount,
        List<InventoryValuationRow> items
) {
    public record InventoryValuationRow(
            Long locationId,
            String locationCode,
            String locationName,
            Long productId,
            String productSku,
            String productName,
            BigDecimal quantityOnHand,
            BigDecimal unitCostPrice,
            BigDecimal unitSellingPrice,
            BigDecimal costValue,
            BigDecimal retailValue
    ) {
    }
}
