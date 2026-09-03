# Restricted Warehouses & Temporary Permissions — Phase 10

## Problem

MDL has **two MAIN warehouses** (`LOC-MAIN`, `LOC-MAIN-B`) marked `is_restricted = TRUE`. Broad permissions like `inventory:view:all` must **not** grant permanent access to these locations. Access should be **task-based** and **time-limited**.

## Solution

### Restricted location rules

| Actor | Non-restricted locations | Restricted MAIN warehouses |
|-------|--------------------------|----------------------------|
| OWNER | Full access | Full access |
| `inventory:view:all` | All locations | Only with active temporary grant |
| Location assignment | Assigned locations | Only with active temporary grant |
| Temporary grant | N/A | Access while grant is active |

`LocationAccessService` enforces these rules for inventory queries, location lists, and import receiving.

### Temporary permissions table (`temporary_permissions`)

Each grant links:

- **user** — who receives access
- **permission_code** — e.g. `import:receive:task`, `inventory:view`
- **location_id** — restricted warehouse location
- **reference_type / reference_id** — optional task link (e.g. `IMPORT` + import id)
- **expires_at** — automatic expiry
- **status** — `ACTIVE`, `REVOKED`, `EXPIRED`

### API

| Method | Path | Permission | Purpose |
|--------|------|------------|---------|
| GET | `/api/security/temporary-permissions` | `permission:grant` or `security:view` | List grants |
| POST | `/api/security/temporary-permissions` | `permission:grant` | Grant access to restricted location |
| POST | `/api/security/temporary-permissions/{id}/revoke` | `permission:grant` | Revoke active grant |

### Import auto-grants

When an import is **approved** and `assignedReceiverUserId` is set:

1. A temporary grant is created for `import:receive:task` at the destination MAIN warehouse
2. Reference: `IMPORT` + import id
3. Default expiry: 7 days

Grants are **revoked** when the import is **verified** or **cancelled**.

Task-scoped receivers must be assigned **and** hold an active grant before receiving stock.

## Demo account

| Email | Password | Role |
|-------|----------|------|
| receiver@mdl.local | Receiver@123! | IMPORT_RECEIVING_STAFF |

No permanent location assignments — access comes only via import task grants.

## Key classes

- `RestrictedWarehouseAccessService` — identifies restricted warehouse locations
- `TemporaryPermissionService` — grant, revoke, list, active-grant checks
- `LocationAccessService` — updated location resolution with restriction rules
- `ImportService` — auto-grant on approve, revoke on verify/cancel

## Migration

`V12__temporary_permissions.sql` — creates `temporary_permissions` table.
