# Point of Sale (Sales) — Phase 12

## Purpose

Complete **POS checkout** at shop locations: sell products, record payments in business currency (GHS for MDL), and deduct stock from the shop's linked warehouse via the inventory ledger.

## Endpoints

| Method | Path | Permission | Description |
|--------|------|------------|-------------|
| GET | `/api/sales` | `sale:view` | List sales (location-scoped) |
| GET | `/api/sales/{id}` | `sale:view` | Sale detail with items and payments |
| POST | `/api/sales` | `sale:create` | Complete a sale (POS checkout) |
| POST | `/api/sales/{id}/cancel` | `sale:cancel` | Cancel sale and restore stock |
| POST | `/api/sales/{id}/refund` | `sale:refund` | Refund sale and restore stock |

## Checkout flow

1. Worker selects **shop** (must have location access)
2. Adds line items — unit price defaults to catalog `sellingPrice` unless overridden
3. Records one or more payments — **total must match** sale amount
4. Stock deducted from shop warehouse → `SALE` ledger entries
5. Currency taken from `businesses.currency_code` (never hard-coded)

## Payment methods

`CASH`, `MOBILE_MONEY`, `CARD`, `BANK_TRANSFER`

## Cancel vs refund

Both restore inventory to the shop warehouse:

| Action | Permission | Ledger type | Status |
|--------|------------|-------------|--------|
| Cancel | `sale:cancel` | `SALE_CANCEL` | `CANCELLED` |
| Refund | `sale:refund` | `SALE_REFUND` | `REFUNDED` |
| Return (partial) | `sale:return` | `RETURN` | `PARTIALLY_RETURNED` / `REFUNDED` |

Only **COMPLETED** sales can be cancelled or fully refunded. Use [returns](returns.md) for partial line-item returns.

See `docs/architecture/returns.md` for the returns API (Phase 15).

## Demo workflow

John (shop worker) sells at Shop A:

```json
POST /api/sales
{
  "shopId": <SHOP-A id>,
  "customerName": "Walk-in",
  "items": [{ "productId": <id>, "quantity": 2 }],
  "payments": [{ "paymentMethod": "CASH", "amount": 370.00 }]
}
```

Michael (shop manager) can cancel with a reason if the customer returns items.

## Barcode lookup

Use existing `GET /api/products/lookup?barcode=` to find products at the POS before creating a sale.

## Migration

`V14__create_sales.sql` — `sales`, `sale_items`, `sale_payments`, role permission grants.
