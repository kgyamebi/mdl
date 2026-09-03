package com.mdl.platform.inventory.service;

import com.mdl.platform.inventory.entity.InventoryBalance;
import com.mdl.platform.inventory.entity.InventoryTransaction;

/** Result of an on-hand ledger movement. */
public record LedgerMovementResult(InventoryTransaction transaction, InventoryBalance balance) {
}
