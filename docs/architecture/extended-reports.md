# Extended analytics reports (Phase 19)

Phase 19 adds read-only analytics endpoints on top of the Phase 13 reporting foundation. No new database tables — all reports are computed from existing sales, inventory, and transfer data.

## Endpoints

All endpoints require `report:view`.

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/reports/sales-by-product` | Revenue and units sold per product (completed sales only) |
| GET | `/api/reports/inventory-valuation` | Cost and retail value of on-hand stock |
| GET | `/api/reports/transfer-activity` | Transfer counts grouped by status |

### Sales by product

Query parameters (all optional):

- `shopId` — limit to one shop
- `from` / `to` — ISO-8601 instant range on sale `created_at`

Returns rows ordered by revenue descending. Only `COMPLETED` sales are included.

### Inventory valuation

Query parameters:

- `locationId` — optional; must be within the caller's accessible locations

Totals:

- **totalCostValue** — sum of `quantity_on_hand × cost_price` (zero cost when product has no cost price)
- **totalRetailValue** — sum of `quantity_on_hand × selling_price`

Only balances with `quantity_on_hand > 0` are included. Location scope follows the same rules as inventory list APIs.

### Transfer activity

Query parameters (optional):

- `from` / `to` — ISO-8601 instant range on transfer `created_at`

Returns `totalTransfers` and `statusCounts` (e.g. `REQUESTED`, `IN_TRANSIT`, `COMPLETED`). Scoped to transfers where the caller can see the from or to location, or all transfers when the user has business-wide location access.

## Design notes

- JSON-only in Phase 19; CSV export for these report types can be added in a later phase by extending `report_exports` types.
- Valuation uses current product prices, not historical cost at time of receipt.
- Sales-by-product aligns with the sales summary report filters for consistent date/shop filtering.

## Related

- [Audit and basic reports](./audit-and-reports.md) — Phase 13
- [Report exports](./report-exports.md) — Phase 18
