# Report Exports — Phase 18

## Purpose

Allow accountants and managers to **download reports as CSV** while keeping an **audit trail** of every export (who, what, when, parameters, row count).

## Export API

| Method | Path | Permission | Description |
|--------|------|------------|-------------|
| GET | `/api/reports/sales-summary/export` | `report:export` | Sales summary CSV |
| GET | `/api/reports/inventory-balances/export` | `report:export` | Inventory balances CSV |
| GET | `/api/reports/low-stock/export` | `report:export` | Low-stock items CSV |
| GET | `/api/reports/exports` | `report:export` | Export history |
| GET | `/api/reports/exports/{id}` | `report:export` | Export metadata |

View-only reports (`report:view`) remain on the existing JSON endpoints from Phase 13.

## CSV formats

**Sales summary** — metric/value pairs (currency, date range, counts, amounts).

**Inventory / low stock** — tabular: location, SKU, product, on-hand, reserved, available, reorder level.

## Export logging

Each download creates a row in `report_exports` with:

- Report type, file name, row count
- JSON parameters (filters used)
- User who exported

Also logged to the main audit trail as `REPORT_EXPORTED`.

## Migration

`V20__create_report_exports.sql` — `report_exports` table. Permission `report:export` was seeded in V3 and granted in V15.

## Design rules

1. **Export requires `report:export`** — stricter than view-only access
2. **Location-scoped inventory exports** — same rules as inventory API
3. **Cap** — inventory exports limited to 5,000 rows per download
4. **UTF-8 CSV** — proper escaping for commas and quotes
