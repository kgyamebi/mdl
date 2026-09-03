# In-App Notifications — Phase 16

## Purpose

Deliver **actionable messages to users** inside the platform — alert summaries, approval requests, and security events — without email/SMS (those come in a later phase).

Each user sees only **their own** notification inbox for the current business.

## API

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/notifications` | List own notifications |
| GET | `/api/notifications/unread-count` | Unread badge count |
| POST | `/api/notifications/{id}/read` | Mark one as read |
| POST | `/api/notifications/{id}/dismiss` | Dismiss notification |
| POST | `/api/notifications/mark-all-read` | Mark all unread as read |

**List filters:** `status` (`UNREAD`, `READ`, `DISMISSED`), `category`, `page`, `size`

## Categories

| Category | Examples |
|----------|----------|
| `ALERT` | Low stock, pending transfers (from business alerts) |
| `SECURITY` | Account locked, failed login burst |
| `APPROVAL` | Stock adjustment awaiting manager approval |
| `SYSTEM` | Reserved for future platform messages |

## Event sources (Phase 16)

| Trigger | Recipients | Permission used |
|---------|------------|-----------------|
| Business alert created/updated | Users with `alert:view` | `alert:view` |
| Adjustment request submitted | Users with `inventory:adjust` | `inventory:adjust` |

Notifications are **deduplicated** per user via `dedupe_key` — repeated alerts update the same unread row instead of spamming the inbox.

## Migration

`V18__create_notifications.sql` — `notifications` table.

## Design rules

1. **User-scoped** — API always filters by authenticated user + business
2. **No hard delete** — dismiss or read, never DELETE rows via API
3. **Decoupled publishing** — modules use `NotificationPublisher` interface
4. **Neutral copy** — factual messages suitable for staff and owners

## Future (later phases)

- Push / email / SMS delivery channels
- Per-user notification preferences
- Mark-as-read on open in UI
