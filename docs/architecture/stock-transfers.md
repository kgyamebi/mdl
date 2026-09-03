# Stock Transfers — Phase 11

## Purpose

Move inventory between warehouses using **authorized routes only**. Shop workers **request** stock; managers **approve**, **dispatch** from source, and workers **receive** at destination.

All movements write `TRANSFER_OUT` / `TRANSFER_IN` ledger entries. Approved transfers **reserve** stock at the source until dispatch consumes the reservation.

## Lifecycle

```
REQUESTED → APPROVED → DISPATCHED → RECEIVED
     ↓           ↓
 REJECTED    CANCELLED
```

| Status | Meaning |
|--------|---------|
| REQUESTED | Worker submitted (`stock:request`) |
| APPROVED | Manager approved; stock reserved at source |
| DISPATCHED | Stock left source (`TRANSFER_OUT`) |
| PARTIALLY_RECEIVED | Some lines received |
| RECEIVED | All dispatched quantity received (`TRANSFER_IN`) |
| REJECTED | Manager declined request |
| CANCELLED | Cancelled before dispatch |

Users with `transfer:create` skip REQUESTED and create directly as **APPROVED** (with immediate reservation).

## Endpoints

| Method | Path | Permission | Description |
|--------|------|------------|-------------|
| GET | `/api/stock-transfers` | `transfer:view` | List transfers (scoped by location) |
| GET | `/api/stock-transfers/{id}` | `transfer:view` | Transfer detail |
| POST | `/api/stock-transfers` | `stock:request` or `transfer:create` | Create request or direct transfer |
| POST | `/api/stock-transfers/{id}/approve` | `transfer:approve` | Approve request + reserve stock |
| POST | `/api/stock-transfers/{id}/reject` | `transfer:approve` | Reject request |
| POST | `/api/stock-transfers/{id}/dispatch` | `transfer:dispatch` | Ship from source warehouse |
| POST | `/api/stock-transfers/{id}/receive` | `transfer:receive` | Receive at destination (partial OK) |
| POST | `/api/stock-transfers/{id}/cancel` | `stock:request`, `transfer:approve`, or `transfer:create` | Cancel before dispatch |

## Rules enforced

1. **Authorized routes only** — `warehouse_transfer_routes` must have an enabled `from → to` pair
2. **Destination access** — requester must have access to the destination location
3. **Source access on approve/dispatch** — approver/dispatcher must access the source (restricted MAIN requires owner or temporary grant)
4. **Reservations** — created on approve; consumed on dispatch; released on cancel
5. **Ledger** — all quantity changes via `InventoryLedgerService`

## Demo workflow

John (shop worker) requests 5× `MDL-LED-001` from `WH-MAIN` → `WH-SHOP-A`:

1. `POST /api/stock-transfers` as john@mdl.local
2. Owner approves and dispatches
3. John receives at Shop A warehouse

## Migration

`V13__create_stock_transfers.sql` — `stock_transfers`, `stock_transfer_items`, role permission grants.
