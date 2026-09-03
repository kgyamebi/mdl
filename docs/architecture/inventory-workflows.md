# Inventory Workflows — Phase 8

## Purpose

Extend the Phase 7 ledger with **approval workflows**, **stock reservations**, and **damage reporting** — preparing for transfers and POS without allowing silent stock changes.

## New endpoints

| Method | Path | Permission | Description |
|--------|------|------------|-------------|
| GET | `/api/inventory/summary` | `inventory:view` | Counts: balances, low stock, pending requests, active holds |
| POST | `/api/inventory/adjustment-requests` | `inventory:adjust:request` | Worker submits adjustment for approval |
| GET | `/api/inventory/adjustment-requests` | `inventory:adjust:request` or `inventory:adjust` | List requests |
| POST | `/api/inventory/adjustment-requests/{id}/approve` | `inventory:adjust` | Manager posts ledger adjustment |
| POST | `/api/inventory/adjustment-requests/{id}/reject` | `inventory:adjust` | Reject without changing stock |
| POST | `/api/inventory/reservations` | `inventory:reserve` | Hold available stock |
| GET | `/api/inventory/reservations` | `inventory:view` | List reservations |
| POST | `/api/inventory/reservations/{id}/release` | `inventory:reserve` | Release a hold |
| POST | `/api/inventory/damage-reports` | `damage:report` | Write off damaged stock (`DAMAGE` ledger row) |

Phase 7 endpoints (`/balances`, `/transactions`, `/adjustments`) remain unchanged.

## Adjustment request flow

```
Shop worker → POST adjustment-request (PENDING)
     ↓
Shop manager / owner → approve → ledger ADJUSTMENT + request APPROVED
                    → reject  → request REJECTED (no stock change)
```

Workers **cannot** post direct adjustments (`inventory:adjust`). They use `inventory:adjust:request`.

## Stock reservations

Reservations increase `quantity_reserved` on the balance — **not** `quantity_on_hand`.

```
Available = quantity_on_hand - quantity_reserved
```

Reservations block sales/transfers from overselling held stock. Future transfer and POS modules will **consume** reservations instead of releasing them.

## Damage reports

Creates a `DAMAGE` transaction (negative quantity change). Validates against **available** stock, not just on-hand — reserved stock cannot be written off without releasing first.

## Ledger core refactor

All on-hand changes go through `InventoryLedgerService.applyOnHandChange()`:

- Validates non-zero change
- Prevents negative on-hand
- Prevents dropping below reserved quantity
- Writes immutable `inventory_transactions` row
- Updates `inventory_balances`

## Permissions added (V10)

| Permission | Roles |
|------------|-------|
| `inventory:reserve` | Owner, General Manager, Warehouse Manager, Shop Manager |
| `inventory:adjust` | Also granted to **Shop Manager** (approve shop requests) |

## Example: worker damage report

```http
POST /api/inventory/damage-reports
Authorization: Bearer <worker-token>

{
  "locationId": 4,
  "productId": 16,
  "quantity": 1,
  "reason": "Water damage on tape roll"
}
```
