# Database Schema — Phase 2

This document describes the database tables created in Flyway migrations V1–V5.

## Migration history

| Version | File | Purpose |
|---------|------|---------|
| V1 | `V1__create_businesses.sql` | Tenants, currencies, MDL seed |
| V2 | `V2__create_users_and_auth.sql` | Users, sessions, devices, MFA-ready |
| V3 | `V3__create_roles_and_permissions.sql` | RBAC foundation + permission seeds |
| V4 | `V4__create_locations_shops_warehouses.sql` | Locations, warehouses, shops, routes |
| V5 | `V5__seed_mdl_locations.sql` | MDL demo structure |
| V6 | `V6__create_products_catalog.sql` | Categories, products, barcodes |
| V7 | `V7__seed_mdl_products.sql` | MDL electrical product catalog |
| V8 | `V8__create_inventory.sql` | Inventory balances + transaction ledger |
| V9 | `V9__seed_mdl_inventory.sql` | MDL demo stock levels |
| V10 | `V10__inventory_workflows.sql` | Adjustment requests, reservations |
| V11 | `V11__create_imports.sql` | Import orders, items, evidence |
| V12 | `V12__temporary_permissions.sql` | Task-based access to restricted warehouses |
| V13 | `V13__create_stock_transfers.sql` | Stock requests and warehouse transfers |
| V14 | `V14__create_sales.sql` | POS sales, items, payments |
| V15 | `V15__create_audit_logs.sql` | Append-only audit trail |
| V16 | `V16__create_business_alerts.sql` | Alerts and owner attention center |
| V17 | `V17__create_sale_returns.sql` | Customer returns against sales |
| V18 | `V18__create_notifications.sql` | In-app user notifications |
| V19 | `V19__create_stocktakes.sql` | Physical stock counts |
| V20 | `V20__create_report_exports.sql` | Report export audit trail |
| V21 | `V21__create_approval_rules.sql` | Configurable approval rules + inbox |
| V22 | `V22__create_approval_workflow.sql` | Multi-step approval chains |

## Entity relationship (Phase 2 scope)

```mermaid
erDiagram
    businesses ||--o{ user_business_memberships : has
    businesses ||--o{ locations : has
    businesses ||--o{ warehouses : has
    businesses ||--o{ shops : has
    businesses ||--o{ roles : may_have
    businesses ||--o{ warehouse_transfer_routes : configures

    users ||--o{ user_business_memberships : belongs
    users ||--o{ user_roles : assigned
    users ||--o{ user_sessions : has
    users ||--o{ user_location_assignments : scoped_to

    roles ||--o{ role_permissions : grants
    permissions ||--o{ role_permissions : included_in
    roles ||--o{ user_roles : assigned_via

    locations ||--o| warehouses : hosts
    locations ||--o| shops : hosts
    warehouses ||--o| shops : supplies
    warehouses ||--o{ warehouse_transfer_routes : from
    warehouses ||--o{ warehouse_transfer_routes : to

    supported_currencies ||--o{ businesses : currency
```

## Table summary

### Tenant & currency

| Table | Rows (MDL seed) | Purpose |
|-------|-----------------|---------|
| `businesses` | 1 (MDL) | Multi-tenant root; `currency_code` is changeable |
| `supported_currencies` | 5 | ISO 4217 reference (GHS, USD, EUR, GBP, NGN) |

### Users & auth

| Table | Purpose |
|-------|---------|
| `users` | Global login identity (can belong to multiple businesses) |
| `user_business_memberships` | Links user ↔ business |
| `user_sessions` | Active sessions for revocation & shared-account detection |
| `user_devices` | Trusted devices (owner MFA) |
| `mfa_credentials` | TOTP / WebAuthn credentials (Phase 3+) |

### Authorization

| Table | Purpose |
|-------|---------|
| `permissions` | 35 fine-grained permissions |
| `roles` | 11 system roles (OWNER, SHOP_WORKER, etc.) |
| `role_permissions` | Role → permission mapping |
| `user_roles` | User → role scoped by business |
| `user_location_assignments` | Location-based access control |

### Locations & warehouses

| Table | MDL seed count | Purpose |
|-------|----------------|---------|
| `locations` | 8 | Physical sites (shops + warehouses) |
| `warehouses` | 5 | 2 MAIN (restricted) + 3 SHOP warehouses |
| `shops` | 3 | Shop A, B, C — each linked to its warehouse |
| `warehouse_transfer_routes` | 8 | Authorized transfer paths only |

### Products (Phase 6)

| Table | MDL seed count | Purpose |
|-------|----------------|---------|
| `product_categories` | 5 | Lighting, switches, cables, protection, accessories |
| `products` | 16 | SKU catalog with GHS pricing (no stock quantity) |
| `barcodes` | 13+ | EAN13 barcodes for POS lookup |

### Inventory (Phase 7)

| Table | MDL seed count | Purpose |
|-------|----------------|---------|
| `inventory_balances` | 12 | Current stock per location + product |
| `inventory_transactions` | 12 | Immutable ledger (opening balances) |

### Inventory workflows (Phase 8)

| Table | Purpose |
|-------|---------|
| `inventory_adjustment_requests` | Worker-submitted changes awaiting manager approval |
| `inventory_reservations` | Active stock holds (reduces available, not on-hand) |

### Authorization (Phase 10)

| Table | Purpose |
|-------|---------|
| `temporary_permissions` | Time-limited grants for restricted main warehouse access |

### Imports (Phase 9)

| Table | Purpose |
|-------|---------|
| `imports` | Supplier shipment orders into MAIN warehouses |
| `import_items` | Expected vs received quantities per product |
| `import_evidence` | Photos, documents, notes linked to imports |

### Stock transfers (Phase 11)

| Table | Purpose |
|-------|---------|
| `stock_transfers` | Warehouse-to-warehouse transfer orders |
| `stock_transfer_items` | Requested, dispatched, and received quantities |

### Sales (Phase 12)

| Table | Purpose |
|-------|---------|
| `sales` | Completed POS sales at shops |
| `sale_items` | Line items with captured unit prices |
| `sale_payments` | Cash, mobile money, card, bank payments |

### Audit (Phase 13)

| Table | Purpose |
|-------|---------|
| `audit_logs` | Immutable record of who did what, when |

### Alerts (Phase 14)

| Table | Purpose |
|-------|---------|
| `business_alerts` | Deduplicated alerts for anomalies and pending work |

### Returns (Phase 15)

| Table | Purpose |
|-------|---------|
| `sale_returns` | Customer return transactions linked to sales |
| `sale_return_items` | Line items and quantities returned |
| `sale_return_refunds` | Refund payments issued to customer |

### Notifications (Phase 16)

| Table | Purpose |
|-------|---------|
| `notifications` | Per-user inbox messages for alerts and workflows |

### Stocktake (Phase 17)

| Table | Purpose |
|-------|---------|
| `stocktakes` | Physical count sessions at a location |
| `stocktake_lines` | Expected vs counted quantities per product |

### Report exports (Phase 18)

| Table | Purpose |
|-------|---------|
| `report_exports` | Log of CSV report downloads |

## MDL warehouse structure

```
MDL (Modern Dream Light) — currency: GHS
│
├── WH-MAIN   (MAIN, restricted)   — Main Import Warehouse
├── WH-MAIN-B (MAIN, restricted)   — Regional Distribution Center
├── WH-SHOP-A (SHOP)  ← Shop A
├── WH-SHOP-B (SHOP)  ← Shop B
└── WH-SHOP-C (SHOP)  ← Shop C
```

**Important:** The system does **not** assume one main warehouse. `warehouse_type = 'MAIN'` is stored as data.

## Design rules enforced in schema

1. **Tenant isolation** — all business tables include `business_id`
2. **No hard-coded currency** — `businesses.currency_code` FK → `supported_currencies`
3. **Multiple main warehouses** — `warehouses.warehouse_type` enum includes `MAIN`
4. **Restricted warehouses** — `warehouses.is_restricted = TRUE` for main warehouses
5. **Transfer routes** — shops cannot freely transfer; only `warehouse_transfer_routes` pairs allowed
6. **Users global, access scoped** — users exist once; membership + roles + locations scope access

## Indexes

Every business-scoped table has an index on `business_id`. Foreign keys enforce referential integrity. Unique constraints prevent duplicate codes within a business.

| V23 | Parallel approval steps (any-of at same step_order) |

## Next migrations (Phase 32+)

| Planned | Tables |
|---------|--------|
| Phase 32+ | Optional UI polish (no new migrations expected) |

## Verify locally

After starting MariaDB:

```sql
USE mdl_platform;

SELECT code, name, currency_code FROM businesses;
SELECT code, warehouse_type, is_restricted FROM warehouses;
SELECT code, name FROM shops;
SELECT COUNT(*) FROM permissions;
SELECT code FROM roles;
```
