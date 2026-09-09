package com.mdl.platform.inventory.dto;

import java.math.BigDecimal;

/**
 * Filter options for browsing inventory balances.
 *
 * <p>{@code minQuantity} and {@code maxQuantity} are inclusive bounds on quantity on hand, so the
 * UI can express "50 or more", "10 or less", and "between 10 and 50" without ambiguity around
 * fractional quantities.
 */
public record InventoryBalanceFilter(
        Long locationId,
        Long productId,
        String search,
        boolean lowStockOnly,
        boolean negativeStockOnly,
        BigDecimal minQuantity,
        BigDecimal maxQuantity) {

    public InventoryBalanceFilter {
        if (minQuantity != null && maxQuantity != null && minQuantity.compareTo(maxQuantity) > 0) {
            BigDecimal swap = minQuantity;
            minQuantity = maxQuantity;
            maxQuantity = swap;
        }
        search = search == null ? "" : search.trim();
    }
}
