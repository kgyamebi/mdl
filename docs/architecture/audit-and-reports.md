# Audit Trail & Reports — Phase 13

## Purpose

Record **who did what, when** for important business and security actions, and provide **read-only summary reports** for owners and managers.

Audit logs are **append-only** — never updated or deleted through the API.

## Audit API

| Method | Path | Permission | Description |
|--------|------|------------|-------------|
| GET | `/api/audit-logs` | `audit:view` | Search audit trail |

**Filters:** `userId`, `module`, `action`, `entityType`, `entityId`, `from`, `to`, `page`, `size`

### Modules logged (Phase 13)

| Module | Example actions |
|--------|-----------------|
| AUTH | `LOGIN_SUCCESS`, `LOGIN_FAILED`, `LOGOUT` |
| SALES | `SALE_CREATED`, `SALE_CANCELLED`, `SALE_REFUNDED` |
| IMPORTS | `IMPORT_APPROVED` |
| TRANSFERS | `TRANSFER_APPROVED`, `TRANSFER_DISPATCHED` |
| SECURITY | `PERMISSION_GRANTED`, `PERMISSION_REVOKED` |

Each entry stores: user, action, module, entity reference, human-readable summary, optional JSON details, IP, user agent, timestamp.

## Reports API

| Method | Path | Permission | Description |
|--------|------|------------|-------------|
| GET | `/api/reports/sales-summary` | `report:view` | Sales totals by status and date range |
| GET | `/api/reports/business-overview` | `report:view` | Owner snapshot: today's sales, low stock, pending requests |

See [report exports](report-exports.md) (Phase 18) for CSV download endpoints and export audit trail.

See [extended analytics reports](extended-reports.md) (Phase 19) for sales-by-product, inventory valuation, and transfer activity.

### Sales summary fields

- Completed / cancelled / refunded counts and amounts
- Items sold (completed sales)
- Net sales (gross minus refunded)
- Currency from business settings

## Migration

`V15__create_audit_logs.sql` — `audit_logs` table + role permission grants for AUDITOR, ACCOUNTANT, SHOP_MANAGER, GENERAL_MANAGER.

## Design rules

1. **Immutable** — no UPDATE/DELETE on audit rows
2. **Server-side only** — logging happens in services, not the frontend
3. **Neutral language** — describe actions factually (supports future anomaly review without auto-accusation)
