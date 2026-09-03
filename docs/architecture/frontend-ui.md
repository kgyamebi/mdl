# Frontend UI — Phase 27+

## Purpose

Deliver **authenticated React modules** on top of the Phase 25 PWA shell — login, navigation, operational screens, and approval actions.

## Architecture

```
BrowserRouter
  └── AuthProvider (JWT in localStorage, /api/auth/me bootstrap)
        └── AppShell (PWA banners)
              ├── /              HomePage (public health check)
              ├── /login         LoginPage
              └── ProtectedRoute
                    └── AppLayout (sidebar nav)
                          ├── /dashboard   DashboardPage
                          ├── /inventory   InventoryPage
                          ├── /products    ProductsPage
                          ├── /transfers   TransfersPage
                          ├── /imports     ImportsPage
                          ├── /sales       SalesPage (POS)
                          └── /approvals   ApprovalsPage (+ approve/reject)
```

## Auth flow

1. `POST /api/auth/login` stores `accessToken`, `refreshToken`, and user profile
2. Authenticated requests send `Authorization: Bearer …`
3. On `401`, the client tries `POST /api/auth/refresh` once, then signs out
4. Navigation visibility uses **`permissions`** from `/api/auth/me`

## Modules

| Route | Permission gate | API |
|-------|-----------------|-----|
| Dashboard | `alert:view` and/or `inventory:view` | `/api/alerts/attention`, `/api/inventory/summary` |
| Inventory | `inventory:view` | `/api/inventory/balances` |
| Products | `product:view` | `/api/products` |
| Transfers | `transfer:view` | `/api/stock-transfers` + lifecycle actions |
| Imports | `import:view` | `/api/imports` + lifecycle actions |
| Sales | `sale:view` | `/api/sales` + POS create, cancel, refund |
| Approvals | `approval:view` | `/api/approvals/inbox` + module approve endpoints |

## Approval actions (Phase 28)

The inbox calls the existing module endpoints based on `entityType`:

| Entity type | Approve | Reject |
|-------------|---------|--------|
| `INVENTORY_ADJUSTMENT` | `POST /api/inventory/adjustment-requests/{id}/approve` | `POST .../reject` |
| `STOCK_TRANSFER` | `POST /api/stock-transfers/{id}/approve` | `POST .../reject` (reason required) |
| `IMPORT_ORDER` | `POST /api/imports/{id}/approve` | — |
| `STOCKTAKE` | `POST /api/inventory/stocktakes/{id}/approve` | — |

Multi-step workflows advance one step per approve; the inbox refreshes after each action.

## Transfers UI (Phase 29)

Full stock transfer lifecycle from the React app:

| Status | Actions (permission-gated) |
|--------|----------------------------|
| `REQUESTED` | Approve / reject (`transfer:approve`), cancel |
| `APPROVED` | Dispatch (`transfer:dispatch`), cancel |
| `DISPATCHED` / `PARTIALLY_RECEIVED` | Receive quantities (`transfer:receive`) |

Workers with `stock:request` can submit new transfer requests; owners with `transfer:create` get auto-approved transfers from the API.

## Imports UI (Phase 30)

Import order lifecycle from the React app:

| Status | Actions (permission-gated) |
|--------|----------------------------|
| `DRAFT` | Submit for approval (`import:create`) |
| `PENDING_APPROVAL` | Approve (`import:approve`) |
| `APPROVED` / `RECEIVING` / `PARTIALLY_RECEIVED` | Receive quantities (`import:receive` or task grant) |
| `RECEIVED` | Verify receiving (`import:verify`) |

Draft imports can be created with supplier, destination location, and line items.

## Sales / POS UI (Phase 31)

Point-of-sale screen for shop staff:

| Feature | Permission | API |
|---------|------------|-----|
| Quick sale | `sale:create` | `POST /api/sales` |
| Sales history | `sale:view` | `GET /api/sales` |
| Cancel sale | `sale:cancel` | `POST /api/sales/{id}/cancel` |
| Refund sale | `sale:refund` | `POST /api/sales/{id}/refund` |

The POS form supports shop selection, one line item, payment method (cash/mobile/card/transfer), and auto-calculated totals from product selling price.

## Development

```powershell
cd frontend
npm install
npm run dev
```

Sign in with demo accounts from root `README.md` (e.g. `owner@mdl.local` / `Owner@123!`).

## Future work

- Notifications inbox UI
- Report export downloads
- Mobile-first bottom navigation

## Related

- [PWA](./pwa.md)
- [Approvals](./approvals.md)
- Root `README.md`
