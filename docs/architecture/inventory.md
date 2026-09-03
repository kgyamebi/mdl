# Inventory Balances — Phase 7

## Purpose

Track **how much stock** exists at each location for each product. Quantities live in `inventory_balances`, not on the product catalog.

Every quantity change writes an immutable row to `inventory_transactions` — the balance is always derived from the ledger.

## Endpoints

| Method | Path | Permission | Description |
|--------|------|------------|-------------|
| GET | `/api/inventory/balances` | `inventory:view` | Paginated balances (`locationId`, `productId`, `search`, `lowStockOnly`) |
| GET | `/api/inventory/balances/{id}` | `inventory:view` | Single balance row |
| GET | `/api/inventory/transactions` | `inventory:view` | Transaction history (`locationId`, `productId`) |
| POST | `/api/inventory/adjustments` | `inventory:adjust` | Manual stock adjustment |

## Location access rules

Same rules as Phase 5 locations:

| User | What they see |
|------|----------------|
| **Owner** | All locations, all balances |
| **Shop worker (John)** | Only Shop A + Shop A Warehouse balances |
| **Direct query for Main Warehouse** | `403 Forbidden` for workers |

## Design rules

1. **Never update `quantity_on_hand` without a transaction** — use `InventoryLedgerService`.
2. **Supports fractional quantities** — cables sold by the metre use `DECIMAL(19,4)`.
3. **Reserved quantity** — `quantity_reserved` holds stock for pending transfers/sales (Phase 8).
4. **Low stock** — `lowStockOnly=true` compares `quantity_on_hand` to product `reorder_level`.

See also [`inventory-workflows.md`](inventory-workflows.md) for Phase 8 approval and reservation flows.

## Transaction types (ledger)

| Type | Phase | Description |
|------|-------|-------------|
| `OPENING_BALANCE` | 7 | Seed / initial stock |
| `ADJUSTMENT` | 7 | Manual correction |
| `IMPORT_RECEIVE` | 9 | Import receiving into main warehouse |
| `TRANSFER_OUT` / `TRANSFER_IN` | 11 | Warehouse transfers (Phase 11) |
| `SALE` / `SALE_CANCEL` / `SALE_REFUND` | 12 | POS sales |

## MDL seed stock (V9)

| Location | Example stock |
|----------|----------------|
| Main Import Warehouse (`LOC-MAIN`) | 500 LED panels, 2000 bulbs, 5000m cable |
| Shop A Warehouse (`LOC-WH-A`) | 120 bulbs, 45 switches, 8 tape rolls (low stock) |
| Shop B Warehouse (`LOC-WH-B`) | 90 bulbs, 20 sockets |

## Example: list Shop A warehouse stock

```http
GET /api/inventory/balances?search=LED
Authorization: Bearer <john-token>
```

## Example: post adjustment (owner)

```http
POST /api/inventory/adjustments
Authorization: Bearer <owner-token>

{
  "locationId": 4,
  "productId": 2,
  "quantityChange": -3,
  "notes": "Damaged units written off"
}
```

Negative `quantityChange` reduces stock; request fails with `409` if stock would go below zero.
