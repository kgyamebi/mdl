# Alerts & Owner Attention — Phase 14

## Purpose

Surface **what needs attention** for owners and managers — low stock, pending approvals, security anomalies — without requiring them to check every module manually.

Alerts are **deduplicated** by `dedupe_key` so the same condition does not spam new rows. When a condition clears, open alerts are auto-resolved.

## API

| Method | Path | Permission | Description |
|--------|------|------------|-------------|
| GET | `/api/alerts/attention` | `alert:view` | Owner attention dashboard |
| GET | `/api/alerts` | `alert:view` | Search persisted alerts |
| POST | `/api/alerts/scan` | `alert:view` | Run detection rules now |
| POST | `/api/alerts/{id}/acknowledge` | `alert:acknowledge` | Mark alert acknowledged |

**List filters:** `status`, `severity`, `alertType`, `module`, `page`, `size`

## Alert types (Phase 14)

| Type | Severity | Trigger |
|------|----------|---------|
| `LOW_STOCK` | WARNING | Balance at or below product reorder level |
| `PENDING_ADJUSTMENTS` | WARNING | Adjustment requests awaiting approval |
| `PENDING_TRANSFERS` | WARNING | Transfer requests in `REQUESTED` status |
| `TRANSFERS_IN_TRANSIT` | INFO | Transfers dispatched, awaiting receipt |
| `PENDING_IMPORT_APPROVAL` | WARNING | Imports in `PENDING_APPROVAL` (owner/manager view) |
| `IMPORTS_AWAITING_RECEIVE` | INFO | Approved imports not yet received |
| `ACCOUNT_LOCKED` | CRITICAL | User locked after repeated failed logins |
| `FAILED_LOGIN_BURST` | WARNING | 3+ failed logins in 15 minutes |

## Attention dashboard

Returns:

- Open / critical / warning counts
- **Categories** — live counts grouped by area (low stock, pending work, security)
- **Recent alerts** — top 5 open or acknowledged alerts

Calling `/attention` runs a scan first so counts stay current.

## Migration

`V16__create_business_alerts.sql` — `business_alerts` table + `alert:view` / `alert:acknowledge` permissions.

## Design rules

1. **Neutral language** — describe conditions factually (e.g. "repeated failed login attempts", not "possible attack")
2. **Dedupe** — one open alert per condition via `dedupe_key`
3. **Auto-resolve** — aggregate alerts resolve when count reaches zero
4. **Security hooks** — failed login and account lock create alerts via `AlertNotifier` from auth
