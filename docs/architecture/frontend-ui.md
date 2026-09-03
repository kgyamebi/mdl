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
                    └── AppLayout (sidebar nav on desktop, bottom nav on mobile)
                          ├── /dashboard   DashboardPage
                          ├── /inventory   InventoryPage
                          ├── /products    ProductsPage
                          ├── /transfers   TransfersPage
                          ├── /imports     ImportsPage
                          ├── /sales       SalesPage (POS)
                          ├── /reports     ReportsPage (CSV exports)
                          ├── /approvals   ApprovalsPage (+ approve/reject)
                          ├── /notifications NotificationsPage (read/dismiss)
                          └── /copilot       CopilotPage (AI assistant)
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
| Reports | `report:export` | `/api/reports/*/export` + export history |
| Approvals | `approval:view` | `/api/approvals/inbox` + module approve endpoints |
| Notifications | *(any authenticated user)* | `/api/notifications` + read/dismiss actions |
| Copilot | `copilot:use` | `/api/copilot/chat` + suggested prompts |

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

## Notifications UI (Phase 32)

Route: `/notifications` — visible to all authenticated users (no special permission).

| Feature | Details |
|---------|---------|
| List | Paginated inbox with status and category filters |
| Actions | Mark read, dismiss, mark all read |
| Nav badge | Unread count on sidebar link |

Service: `frontend/src/services/notificationsService.ts`

## Report exports UI (Phase 33)

Route: `/reports` — requires `report:export`.

| Feature | Details |
|---------|---------|
| Downloads | Sales summary, inventory balances, low stock (CSV) |
| Filters | Shop/date range for sales; location and low-stock toggle for inventory |
| History | Paginated audit log from `/api/reports/exports` |

Service: `frontend/src/services/reportsService.ts`

## Mobile navigation (Phase 34)

On viewports **≤768px** the sidebar is hidden and replaced with:

| Element | Details |
|---------|---------|
| Top bar | Business name and signed-in user |
| Bottom nav | Home, Stock, Sales, Inbox (permission-filtered) |
| More sheet | Overflow modules, user info, sign out |

Uses `env(safe-area-inset-*)` for notched phones. Desktop keeps the sidebar unchanged.

Config: `frontend/src/config/navItems.ts`

## AI Copilot (Phase 35)

Route: `/copilot` — requires `copilot:use`.

| Feature | Details |
|---------|---------|
| Chat | POST `/api/copilot/chat` with conversation history |
| Prompts | Permission-filtered suggested questions on empty state |
| Scope | Inventory, sales, transfers, imports, approvals, notifications |
| Nav | Highlighted sidebar link, mobile More menu, floating action button |

Service: `frontend/src/services/copilotService.ts`

## UX polish (Phase 36)

Frontend-only refinements for non-technical business users:

| Area | Improvements |
|------|----------------|
| Bottom nav | Icons, larger touch targets, safe-area insets |
| Dashboard | At-a-glance summary cards (sales, stock, transfers, approvals, notifications) |
| Tables | Stacked card layout on mobile — no horizontal scroll |
| Forms | Touch-friendly spacing, 16px inputs (prevents iOS zoom) |
| Notifications | Improved empty state |
| PWA | Clearer install prompt wording |
| Accessibility | Larger buttons, improved contrast and spacing |

## Desktop UX (Phase 37)

Frontend-only refinements for laptop and desktop screens (≥769px):

| Area | Improvements |
|------|----------------|
| Layout | Wider content area (up to 1440px), sticky sidebar with grouped navigation |
| Navigation | Overview / Operations / Logistics / Management sections with icons |
| Dashboard | Welcome strip with role-aware quick actions; five-column summary row on large screens |
| Master-detail | Sales, transfers, and imports use side-by-side list + detail panes (≥1280px) |
| Tables | Sticky column headers and comfortable row padding on desktop |
| Copilot | Two-column layout with prompt sidebar; floating button hidden (sidebar link used instead) |

## Related

- [Copilot](./copilot.md)

- [PWA](./pwa.md)
- [Approvals](./approvals.md)
- Root `README.md`
