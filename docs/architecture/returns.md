# Customer Returns — Phase 15

## Purpose

Process **partial customer returns** against completed sales — restore stock to the shop warehouse, refund the customer, and track return history separately from full cancel/refund.

## Endpoints

| Method | Path | Permission | Description |
|--------|------|------------|-------------|
| POST | `/api/sales/{saleId}/returns` | `sale:return` | Process a partial or full return |
| GET | `/api/sales/{saleId}/returns` | `sale:view` | Returns for one sale |
| GET | `/api/sale-returns` | `sale:view` | List all returns (location-scoped) |
| GET | `/api/sale-returns/{id}` | `sale:view` | Return detail |

## Return flow

1. Manager selects a **COMPLETED** or **PARTIALLY_RETURNED** sale
2. Specifies line items and quantities to return (cannot exceed remaining qty)
3. Records refund payment(s) — total must match return amount
4. Stock restored via `RETURN` ledger entries (reference `SALE_RETURN`)
5. Sale status updates:
   - **PARTIALLY_RETURNED** — some items remain sold
   - **REFUNDED** — all quantities fully returned

## Return reasons

`DEFECTIVE`, `WRONG_ITEM`, `CUSTOMER_CHANGED_MIND`, `OTHER`

## Cancel / refund vs returns

| Action | Scope | Permission |
|--------|-------|------------|
| Cancel | Entire sale (COMPLETED only) | `sale:cancel` |
| Full refund | Entire sale (COMPLETED only) | `sale:refund` |
| Return | Partial or full line items | `sale:return` |

Full cancel/refund is blocked once a sale is **PARTIALLY_RETURNED** — use returns for remaining items.

## Migration

`V17__create_sale_returns.sql` — `sale_returns`, `sale_return_items`, `sale_return_refunds`, `quantity_returned` on `sale_items`, `returned_amount` on `sales`, `sale:return` permission.
