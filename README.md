# MDL Platform

**Modern Dream Light (MDL)** — production-quality business management platform for inventory-based businesses.

Built as a **modular monolith**: one application, clear module boundaries, expandable to multi-tenant SaaS later.

---

## Tech stack

| Layer | Technology |
|-------|------------|
| Backend | Java 21, Spring Boot 3.3, Spring Security, JPA, Flyway |
| Database | MariaDB |
| Frontend | React 18, TypeScript, Vite |
| API | REST / JSON |

---

## Prerequisites

Install these before running locally:

1. **Java 21** — [Adoptium Temurin](https://adoptium.net/)
2. **Maven 3.9+** — or use the Maven wrapper (added in a later phase)
3. **Node.js 20 LTS** — [nodejs.org](https://nodejs.org/)
4. **Docker Desktop** (recommended) — for MariaDB
5. **Git**

Verify installation:

```powershell
java -version
mvn -version
node -version
docker --version
```

---

## Quick start

### 1. Clone and configure

```powershell
cd C:\Users\user\Projects\mdl-platform
copy .env.example .env
# Edit .env if needed — defaults work with docker-compose
```

### 2. Start MariaDB

```powershell
docker compose up -d
```

### 3. Start backend

```powershell
cd backend
mvn spring-boot:run
```

Backend runs at `http://localhost:8080`

Health check: `GET http://localhost:8080/api/health`

### 4. Start frontend

```powershell
cd frontend
npm install
npm run dev
```

Frontend runs at `http://localhost:5173` — sign in at `/login` for dashboard, inventory, approvals, notifications, and reports.

### 5. Run tests

```powershell
cd backend
mvn test
```

Unit tests run without Docker. Integration tests (`FlywayMigrationIntegrationTest`) require Docker and are skipped automatically if Docker is unavailable.

---

## Project structure

```
mdl-platform/
├── backend/          Spring Boot API
├── frontend/         React + TypeScript UI
├── docs/             Architecture & design docs
├── docker-compose.yml
├── .env.example
└── README.md
```

---

## Environment variables

All secrets and environment-specific values use environment variables — never hard-coded.

See `.env.example` for the full list.

| Variable | Purpose | Default |
|----------|---------|---------|
| `DB_HOST` | MariaDB host | `localhost` |
| `DB_NAME` | Database name | `mdl_platform` |
| `DB_USER` / `DB_PASSWORD` | DB credentials | see `.env.example` |
| `CORS_ALLOWED_ORIGINS` | Frontend URL(s) | `http://localhost:5173` |
| `JWT_SECRET` | Auth signing key (Phase 3+) | change in production |

---

## Business context

- **Initial tenant:** MDL (Modern Dream Light) — electrical products business
- **Default currency:** GHS (Ghana Cedi)
- **Currency is configurable** per business — see `docs/architecture/currency-design.md`

---

## Development phases

| Phase | Status | Description |
|-------|--------|-------------|
| 1 | ✅ Complete | Project setup |
| 2 | ✅ Complete | Database design (V1–V5 migrations) |
| 3 | ✅ Complete | Authentication (JWT, login, sessions) |
| 4 | ✅ Complete | Users & roles API |
| 5 | ✅ Complete | Businesses, shops & warehouses API |
| 6 | ✅ Complete | Products catalog API |
| 7 | ✅ Complete | Inventory balances + ledger API |
| 8 | ✅ Complete | Adjustment requests, reservations, damage reports |
| 9 | ✅ Complete | Imports API (receive → ledger) |
| 10 | ✅ Complete | Restricted warehouses + temporary permissions |
| 11 | ✅ Complete | Stock requests and warehouse transfers |
| 12 | ✅ Complete | POS / sales API |
| 13 | ✅ Complete | Audit trail + basic reports |
| 14 | ✅ Complete | Alerts + owner attention dashboard |
| 15 | ✅ Complete | Customer returns (partial) |
| 16 | ✅ Complete | In-app notifications |
| 17 | ✅ Complete | Stocktake (physical counts) |
| 18 | ✅ Complete | Report exports + export logging |
| 19 | ✅ Complete | Extended analytics reports |
| 20 | ✅ Complete | Approval engine (rules + unified inbox) |
| 21 | ✅ Complete | Multi-step approval workflows |
| 22 | ✅ Complete | Production readiness (Actuator, security headers) |
| 23 | ✅ Complete | Threshold-based approval routing |
| 24 | ✅ Complete | Approval workflow wired to all modules |
| 25 | ✅ Complete | PWA foundation (installable app shell) |
| 26 | ✅ Complete | Parallel approval steps (any-of approvers) |
| 27 | ✅ Complete | React UI modules (login, dashboard, inventory, approvals) |
| 28 | ✅ Complete | Approval actions + products catalog UI |
| 29 | ✅ Complete | Stock transfers UI (list, create, lifecycle actions) |
| 30 | ✅ Complete | Import orders UI (list, create, receive, verify) |
| 31 | ✅ Complete | POS / sales UI (quick sale, history, cancel/refund) |
| 32 | ✅ Complete | Notifications inbox UI (filters, read/dismiss, unread badge) |
| 33 | ✅ Complete | Report export downloads UI (CSV + export history) |
| 34 | ✅ Complete | Mobile-first bottom navigation (PWA-friendly) |
| 35 | ✅ Complete | MDL AI Copilot (permission-aware business assistant) |
| 36 | ✅ Complete | UX polish & mobile navigation refinement |
| 37 | ✅ Complete | Desktop UI improvements (layout, master-detail, Copilot) |
| 38 | ✅ Complete | Admin UI (users, settings, locations, approval rules, stocktakes) |
| 39 | ✅ Complete | CI pipeline + security hardening (rate limit, MFA, httpOnly refresh) |
| 40 | ✅ Complete | POS multi-line cart + barcode lookup |

---

## License

Private — Modern Dream Light business use.
