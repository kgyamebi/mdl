# Approval engine — Phase 20

## Purpose

Provide a **configurable approval policy layer** and a **unified inbox** so managers see everything awaiting their sign-off in one place — adjustments, transfers, imports, and stocktakes.

Existing module workflows (Phase 8–17) still own approve/reject actions on their own endpoints. Phase 20 adds configuration and visibility, not a replacement approval API.

## Database

**V21 — `approval_rules`**

| Column | Purpose |
|--------|---------|
| `entity_type` | `INVENTORY_ADJUSTMENT`, `STOCK_TRANSFER`, `IMPORT_ORDER`, `STOCKTAKE` |
| `required_permission` | Permission a user must hold to action items of this type |
| `min_abs_quantity` | Optional threshold for future quantity-based routing |
| `priority` | Lower number = higher priority when multiple rules match |
| `enabled` | Soft toggle without deleting the rule |

MDL is seeded with four default rules matching current hard-coded permissions.

## Permissions

| Permission | Purpose |
|------------|---------|
| `approval:view` | View inbox and read rules |
| `approval:manage` | Create and update approval rules (already in V3) |

## API

| Method | Path | Permission | Description |
|--------|------|------------|-------------|
| GET | `/api/approvals/inbox` | `approval:view` | Unified pending items the caller can action |
| GET | `/api/approvals/rules` | `approval:view` or `approval:manage` | List rules for the business |
| GET | `/api/approvals/rules/{id}` | `approval:view` or `approval:manage` | Rule detail |
| POST | `/api/approvals/rules` | `approval:manage` | Create rule |
| PATCH | `/api/approvals/rules/{id}` | `approval:manage` | Update rule |

### Inbox filters

- `entityType` — optional filter to one workflow type
- `page` / `size` — pagination over merged results (newest first)

Inbox items include only workflows where:

1. The item is in a pending state for that module, and
2. The caller has the **required permission** from the active rule (or system default).

### Rule resolution

`ApprovalRuleService.resolveRequiredPermission()` returns the `required_permission` from the highest-priority enabled rule for an entity type, or falls back to:

| Entity type | Default permission |
|-------------|-------------------|
| `INVENTORY_ADJUSTMENT`, `STOCKTAKE` | `inventory:adjust` |
| `STOCK_TRANSFER` | `transfer:approve` |
| `IMPORT_ORDER` | `import:approve` |

## Audit

Rule create/update actions are logged to `audit_logs` under module `APPROVALS`.

## Parallel steps (Phase 26)

Multiple rows with the **same `step_order`** define a parallel **any-of** group — the first eligible approver completes that step.

| step_order | required_permission | Meaning |
|------------|---------------------|---------|
| 1 | `inventory:adjust` | Shop/warehouse manager can approve |
| 1 | `approval:manage` | Owner can also approve (either completes step 1) |
| 2 | `approval:manage` | Owner sign-off after step 1 |

`totalSteps` counts **distinct step orders**, not individual permission rows. The inbox and instance API expose `parallelStep` and `currentStepPermissions` / `requiredPermissions` when a group has multiple options.

Configure parallel groups via `PUT /api/approvals/rules/{id}/steps` — duplicate `stepOrder` values with different permissions are allowed; duplicate `(stepOrder, permission)` pairs are rejected.

## Future work

- Additional module screens (transfers, imports, POS)

See [Frontend UI](./frontend-ui.md) for the Phase 27–28 shell and roadmap.

## Workflow integration (Phase 24)

The approval engine is now wired into all four workflow types:

| Entity type | Trigger | Routing quantity |
|-------------|---------|------------------|
| `INVENTORY_ADJUSTMENT` | Create adjustment request | `abs(requestedChange)` |
| `STOCK_TRANSFER` | Worker stock request (`REQUESTED`) | Sum of item quantities |
| `IMPORT_ORDER` | Submit import for approval | Sum of expected item quantities |
| `STOCKTAKE` | Submit stocktake | `abs(totalVariance)` |

Owner/manager **approve** endpoints advance the workflow step-by-step. Business effects (ledger update, reservations, temp permissions) run only after the **final** step completes. Direct-create transfers (`transfer:create`) skip workflow and remain auto-approved.

## Threshold-based routing (Phase 23)

Rules can set **`minAbsQuantity`** to apply only when the submitted quantity meets or exceeds the threshold. When a worker submits an inventory adjustment, the platform:

1. Takes `abs(requestedChange)` as the routing quantity
2. Evaluates enabled rules for `INVENTORY_ADJUSTMENT` in **priority order** (lower number = higher priority)
3. Selects the **first rule whose threshold matches** — rules with `minAbsQuantity = null` act as catch-alls
4. Starts the approval workflow using that rule's steps

### Example

| Rule | Priority | minAbsQuantity | Steps |
|------|----------|----------------|-------|
| `ADJ-HIGH` | 10 | 10 | Manager → Owner |
| `ADJ-DEFAULT` | 100 | null | Manager only |

- Adjustment of **−2** → `ADJ-DEFAULT`
- Adjustment of **−12** → `ADJ-HIGH`

Configure thresholds via `POST /api/approvals/rules` or `PATCH /api/approvals/rules/{id}` with `minAbsQuantity`.

## Multi-step workflows (Phase 21)

Sequential approval chains are configured per rule via **steps**:

| Table | Purpose |
|-------|---------|
| `approval_rule_steps` | Ordered steps with required permission per step |
| `approval_instances` | Tracks workflow state for a submitted entity |
| `approval_instance_actions` | Audit of each step approve/reject |

### Step API

| Method | Path | Permission |
|--------|------|------------|
| GET | `/api/approvals/rules/{id}/steps` | `approval:view` or `approval:manage` |
| PUT | `/api/approvals/rules/{id}/steps` | `approval:manage` |
| GET | `/api/approvals/instances/{entityType}/{entityId}` | `approval:view` or `approval:manage` |

When a worker submits an **inventory adjustment**, an approval instance starts at step 1. Each `POST .../approve` advances the workflow; the ledger update runs only after the final step. Rejection at any step cancels the workflow and rejects the underlying request.

The inbox (Phase 20) now shows **current step** info and only lists items where the caller can act on the active step.

## Related

- [Inventory workflows](./inventory-workflows.md)
- [Notifications](./notifications.md) — approval request alerts
- [Alerts](./alerts.md) — pending counts on attention dashboard
