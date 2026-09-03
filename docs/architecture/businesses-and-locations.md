# Businesses, Shops & Warehouses — Phase 5

## Purpose

Expose MDL's physical structure through the API: business settings, shops, warehouses (including multiple MAIN warehouses), and authorized transfer routes.

## Endpoints

### Business

| Method | Path | Permission | Description |
|--------|------|------------|-------------|
| GET | `/api/business` | `business:view` | Current business profile |
| PUT | `/api/business` | `business:manage` | Update name, currency, timezone |
| GET | `/api/business/currencies` | `business:view` | Supported ISO currencies |
| GET | `/api/business/structure` | `business:view` + full access | Owner overview of all locations |

### Locations, warehouses, shops

| Method | Path | Permission | Description |
|--------|------|------------|-------------|
| GET | `/api/locations` | `inventory:view` | Locations user can access |
| GET | `/api/locations/{id}` | `inventory:view` | Single location |
| GET | `/api/warehouses` | `inventory:view` | Warehouses (optional `?type=MAIN`) |
| GET | `/api/warehouses/{id}` | `inventory:view` | Single warehouse |
| GET | `/api/shops` | `inventory:view` | Shops user can access |
| GET | `/api/shops/{id}` | `inventory:view` | Single shop |

### Transfer routes

| Method | Path | Permission | Description |
|--------|------|------------|-------------|
| GET | `/api/transfer-routes` | `transfer:view` | Authorized routes |
| POST | `/api/transfer-routes` | `business:manage` | Create route |
| PUT | `/api/transfer-routes/{id}` | `business:manage` | Enable/disable route |

## Location access rules

| User | What they see |
|------|----------------|
| **Owner** | All locations, all warehouses, all shops |
| **Shop worker (John)** | Only assigned locations (Shop A + Shop A Warehouse) |
| **Direct API call to Main Warehouse** | `403 Forbidden` for workers |

## Change currency example

```http
PUT /api/business
Authorization: Bearer <owner-token>

{
  "name": "Modern Dream Light",
  "legalName": "Modern Dream Light",
  "currencyCode": "GHS",
  "timezone": "Africa/Accra"
}
```

Supported codes come from `supported_currencies` (GHS, USD, EUR, GBP, NGN).

## MDL structure (seed data)

```
MDL — GHS
├── WH-MAIN   (MAIN, restricted)
├── WH-MAIN-B (MAIN, restricted)
├── WH-SHOP-A / SHOP-A
├── WH-SHOP-B / SHOP-B
└── WH-SHOP-C / SHOP-C
```

Multiple MAIN warehouses are first-class — the system never assumes only one.
