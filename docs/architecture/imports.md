# Imports — Phase 9

## Purpose

Manage **supplier shipments** into MAIN warehouses — the primary way MDL receives bulk electrical stock from China and other suppliers.

Receiving posts `IMPORT_RECEIVE` rows to the inventory ledger (Phase 7).

## Import lifecycle

```
DRAFT → submit → PENDING_APPROVAL → approve → APPROVED
     → receive (partial OK) → RECEIVING / PARTIALLY_RECEIVED / RECEIVED
     → verify → VERIFIED

cancel: DRAFT | PENDING_APPROVAL | APPROVED (only if nothing received yet)
```

## Endpoints

| Method | Path | Permission | Description |
|--------|------|------------|-------------|
| GET | `/api/imports` | `import:view` | List imports (`?status=`) |
| GET | `/api/imports/{id}` | `import:view` | Import detail with line items |
| POST | `/api/imports` | `import:create` | Create draft import |
| POST | `/api/imports/{id}/submit` | `import:create` | Submit for approval |
| POST | `/api/imports/{id}/approve` | `import:approve` | Approve import |
| POST | `/api/imports/{id}/receive` | `import:receive` or `import:receive:task` | Receive stock (partial OK) |
| POST | `/api/imports/{id}/verify` | `import:verify` | Owner/manager confirms receipt |
| POST | `/api/imports/{id}/cancel` | `import:create` or `import:approve` | Cancel before receiving |
| GET | `/api/imports/{id}/evidence` | `import:view` | List photos/notes/documents |
| POST | `/api/imports/{id}/evidence` | `import:view` | Add evidence record |

## Design rules

1. **MAIN warehouses only** — imports cannot target shop warehouses.
2. **Ledger on receive** — each receive line calls `InventoryLedgerService.applyOnHandChange()` with type `IMPORT_RECEIVE`.
3. **Partial receiving** — cumulative `received_quantity` per line; status tracks progress.
4. **Task-scoped receiving** — users with only `import:receive:task` may receive if assigned via `assignedReceiverUserId`.
5. **Currency** — unit costs stored on line items; business currency applies (from `businesses.currency_code`).

## Example: create import

```http
POST /api/imports
Authorization: Bearer <owner-token>

{
  "supplierName": "Shenzhen LED Supplies Co.",
  "supplierReference": "PO-2026-8842",
  "destinationLocationId": 1,
  "expectedArrivalDate": "2026-09-15",
  "notes": "Container shipment",
  "items": [
    {
      "productId": 1,
      "expectedQuantity": 500,
      "unitCost": 145.00
    }
  ]
}
```

## Example: receive stock

```http
POST /api/imports/1/receive

{
  "items": [
    { "itemId": 1, "quantityReceived": 500, "notes": "All cartons intact" }
  ]
}
```

## Permissions by role

| Role | Capabilities |
|------|--------------|
| Owner | Full import lifecycle |
| General Manager | Create, view, receive, verify, approve |
| Warehouse Manager | Create, view, receive |
| Import Receiving Staff | View + task-scoped receive only |

## Evidence (Phase 9)

Evidence records store metadata (type, description, optional URI). File upload storage is deferred to a later phase — use `referenceUri` for external links or future file keys.
