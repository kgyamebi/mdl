# Architecture Overview — MDL Platform

## Purpose

Help business owners **know what they have, where it is, what was sold, and what needs attention** — with strong security, auditability, and fraud-loss prevention built in.

## Pattern: Modular Monolith

```
React Frontend  →  REST API  →  Spring Boot Modules  →  MariaDB
```

One deployable unit. Modules communicate via service interfaces, not direct repository access across boundaries.

## Multi-tenant & expandable

Every business-scoped record includes `business_id`. MDL is the first tenant (`code = 'MDL'`).

Future businesses (hardware suppliers, PPE, electronics, etc.) are added as new rows — not new code deployments.

## Core modules (planned)

| Module | Responsibility |
|--------|----------------|
| auth / security | Login, JWT, MFA-ready |
| authorization | Role + permission + location + task |
| businesses | Tenant settings, currency, lockdown |
| locations | Shops, warehouses (multiple MAIN types) |
| products | Catalog, barcodes, pricing |
| inventory | Balances + immutable transaction ledger |
| imports | Import lifecycle, receiving, verification |
| transfers | Distribution, in-transit, discrepancies |
| sales | POS, payments, cancellations |
| approvals | Configurable approval rules |
| audit | Immutable audit trail |
| alerts | Anomaly detection, owner attention center |
| reports | Exports with logging |

## Key design rules

1. **Never** represent inventory as `products.quantity` alone
2. **Never** enforce security only in the frontend
3. **Never** hard-delete business records — use cancel/void/reverse
4. **Never** hard-code currency — use `businesses.currency_code`
5. Main warehouse access is **task-based**, not a broad permission flag

## Package structure

```
com.mdl.platform/
├── config/
├── security/
├── auth/
├── authorization/
├── businesses/
├── locations/
├── products/
├── inventory/
├── imports/
├── transfers/
├── sales/
├── approvals/
├── audit/
├── alerts/
├── notifications/
├── copilot/
├── reports/
└── common/
```

## Phase roadmap

See root `README.md` for the full phase plan (through phase 35).
