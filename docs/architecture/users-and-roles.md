# Users & Roles API — Phase 4

## Purpose

Allow the MDL owner to manage workers: create accounts, assign roles, and restrict access to specific shops/warehouses.

All permission checks happen **on the server** — a worker calling the API directly still gets `403 Forbidden` if they lack permission.

## Endpoints

### Users

| Method | Path | Permission | Description |
|--------|------|------------|-------------|
| GET | `/api/users` | `user:view` | List users in current business |
| GET | `/api/users/{id}` | `user:view` | Get one user |
| POST | `/api/users` | `user:manage` | Create user |
| PUT | `/api/users/{id}` | `user:manage` | Update name/phone |
| PUT | `/api/users/{id}/status` | `user:manage` | Activate/deactivate |
| PUT | `/api/users/{id}/roles` | `user:manage` | Assign roles |
| PUT | `/api/users/{id}/locations` | `user:manage` | Assign shop/warehouse access |

### Roles & permissions

| Method | Path | Permission | Description |
|--------|------|------------|-------------|
| GET | `/api/roles` | `user:view` or `user:manage` | List system roles |
| GET | `/api/roles/permissions` | `user:manage` | List all permissions |

## Demo accounts (auto-seeded)

| Email | Password | Role | Locations |
|-------|----------|------|-----------|
| `owner@mdl.local` | `Owner@123!` | OWNER | All (via permissions) |
| `john@mdl.local` | `Worker@123!` | SHOP_WORKER | Shop A, Shop A Warehouse |
| `michael@mdl.local` | `Manager@123!` | SHOP_MANAGER | Shop A, Shop A Warehouse |

Disable demo seeding: `DEMO_SEED_ENABLED=false`

## Example: create a shop worker

```http
POST /api/users
Authorization: Bearer <owner-token>
Content-Type: application/json

{
  "email": "sarah@mdl.local",
  "username": "sarah",
  "password": "Worker@123!",
  "firstName": "Sarah",
  "lastName": "Boateng",
  "phone": "0244000000",
  "roleCodes": ["SHOP_WORKER"],
  "locationIds": [3, 4]
}
```

## Example: assign locations

```http
PUT /api/users/5/locations
Authorization: Bearer <owner-token>

{
  "locations": [
    { "locationId": 3, "accessLevel": "FULL" },
    { "locationId": 4, "accessLevel": "FULL" }
  ]
}
```

## Safety rules

- Only **OWNER** can assign `OWNER` or `SUPER_ADMIN` roles
- Users cannot remove their own **OWNER** role
- Users cannot deactivate their own account
- All queries are scoped to the **business in the JWT** — tenant isolation

## Architecture

```
UserController / RoleController
        ↓
UserManagementService / RoleService
        ↓
AuthorizationService (permission check)
        ↓
Repositories (users, roles, locations)
        ↓
MariaDB
```
