# Stocktake (Physical Counts) — Phase 17

## Purpose

Run **physical stock counts** at a location, compare counted quantities to system expected balances, and apply variances to the inventory ledger only after **manager approval**.

## Endpoints

| Method | Path | Permission | Description |
|--------|------|------------|-------------|
| POST | `/api/inventory/stocktakes` | `stock:count` | Start a stocktake |
| GET | `/api/inventory/stocktakes` | `stock:count` | List stocktakes |
| GET | `/api/inventory/stocktakes/{id}` | `stock:count` | Detail with lines |
| POST | `/api/inventory/stocktakes/{id}/lines` | `stock:count` | Record/update count for a product |
| POST | `/api/inventory/stocktakes/{id}/submit` | `stock:count` | Submit for approval |
| POST | `/api/inventory/stocktakes/{id}/approve` | `inventory:adjust` | Apply variances to ledger |
| POST | `/api/inventory/stocktakes/{id}/cancel` | `stock:count` | Cancel in-progress or submitted |

## Workflow

1. **Start** — optional `preloadBalances: true` seeds lines from current on-hand balances
2. **Count** — worker enters `countedQuantity` per product (can add products not preloaded)
3. **Submit** — computes variance (`counted − expected`) per line; status → `SUBMITTED`
4. **Approve** — manager applies non-zero variances via `STOCKTAKE` ledger entries; status → `COMPLETED`

## Statuses

| Status | Meaning |
|--------|---------|
| `IN_PROGRESS` | Counting underway |
| `SUBMITTED` | Awaiting manager approval |
| `COMPLETED` | Variances posted to ledger |
| `CANCELLED` | Voided |

## Integrations

- **Audit** — `STOCKTAKE_SUBMITTED`, `STOCKTAKE_APPROVED`
- **Notifications** — managers with `inventory:adjust` notified on submit

## Migration

`V19__create_stocktakes.sql` — `stocktakes`, `stocktake_lines` + `stock:count` role grants.

## Design rules

1. **Expected snapshot** — `expected_quantity` captured when line is created, not live-updated
2. **Ledger only on approve** — no balance change until manager approves
3. **Zero variance lines** — skipped on approve (no ledger row)
4. **Location-scoped** — users only access stocktakes at assigned locations
