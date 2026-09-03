# Products Catalog — Phase 6

## Purpose

Manage the product catalog — categories, SKUs, pricing, and barcodes. This is **not** inventory: stock quantities are tracked separately in Phase 7+ via `inventory_balances`.

## Endpoints

### Categories

| Method | Path | Permission | Description |
|--------|------|------------|-------------|
| GET | `/api/product-categories` | `product:view` | List categories (`?activeOnly=true` default) |
| GET | `/api/product-categories/{id}` | `product:view` | Single category |
| POST | `/api/product-categories` | `product:manage` | Create category |
| PUT | `/api/product-categories/{id}` | `product:manage` | Update category |

### Products

| Method | Path | Permission | Description |
|--------|------|------------|-------------|
| GET | `/api/products` | `product:view` | Paginated list (`search`, `categoryId`, `status`) |
| GET | `/api/products/lookup?barcode=` | `product:view` | Find product by barcode (POS-ready) |
| GET | `/api/products/{id}` | `product:view` | Product detail with barcodes |
| POST | `/api/products` | `product:manage` | Create product |
| PUT | `/api/products/{id}` | `product:manage` | Update product |
| POST | `/api/products/{id}/barcodes` | `product:manage` | Add barcode |
| DELETE | `/api/products/{id}/barcodes/{barcodeId}` | `product:manage` | Remove barcode |

## Design rules

1. **No quantity on products** — catalog only; inventory is a separate ledger (Phase 7).
2. **Currency from business** — prices are stored as numbers; API responses include `currencyCode` from `businesses.currency_code` (GHS for MDL).
3. **Tenant isolation** — all queries scoped by JWT `businessId`.
4. **Unique SKU per business** — `MDL-LED-001` style codes.
5. **Unique barcode per business** — supports EAN13, UPC, internal codes.

## MDL seed catalog (V7)

| Category | Example products |
|----------|------------------|
| Lighting | LED Panel 60x60, LED Bulb 12W, LED Tube 4ft |
| Switches & Sockets | 1-Gang Switch, Double Socket, USB Socket |
| Cables & Wiring | 2.5mm / 4mm Twin & Earth (per metre) |
| Circuit Protection | MCB 32A, MCB 63A, RCBO 40A |
| Tools & Accessories | Electrical tape, junction box |

16 products seeded with GHS prices and 13+ barcodes (628 GS1 prefix).

## Example: barcode lookup (for future POS)

```http
GET /api/products/lookup?barcode=6281234567002
Authorization: Bearer <token>
```

Response includes `sku`, `name`, `sellingPrice`, and `currencyCode`.

## Permissions

| Role | Access |
|------|--------|
| Owner / Shop Manager / General Manager | View + manage |
| Shop Worker / Sales Staff | View only |
| Viewer / Accountant / Auditor | View only |
