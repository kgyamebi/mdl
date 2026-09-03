# Observability & deployment — Phase 43

## Purpose

Production operations support: request correlation IDs, Prometheus metrics, structured logging, and containerized full-stack deployment.

## Observability

### Correlation IDs

Every HTTP request receives an `X-Correlation-Id` header:

- Reuses client-provided value when present
- Otherwise generates a UUID
- Logged via MDC key `correlationId` in all log lines for that request

**Filter:** `backend/src/main/java/com/mdl/platform/config/CorrelationIdFilter.java`

### Logging

Logback pattern includes correlation ID:

```
2026-09-03T14:00:00.000+00:00 [http-nio-8080-exec-1] INFO  c.m.p.s.SaleService correlationId=abc-123 - ...
```

**Config:** `backend/src/main/resources/logback-spring.xml`

### Prometheus metrics

Micrometer Prometheus registry exposes JVM, HTTP, and Spring Boot metrics at:

| Endpoint | Auth | Purpose |
|----------|------|---------|
| `/actuator/prometheus` | Public (restrict via network in prod) | Scrape target for Prometheus/Grafana |

**Dependency:** `micrometer-registry-prometheus` in `backend/pom.xml`

## Docker deployment

### Files

| File | Purpose |
|------|---------|
| `backend/Dockerfile` | Multi-stage Java 21 build |
| `frontend/Dockerfile` | Node build + nginx static serve |
| `frontend/nginx.conf` | SPA routing + `/api` and `/actuator` proxy to backend |
| `docker-compose.stack.yml` | Full stack: MariaDB + backend + frontend |

### Quick start (full stack)

```powershell
docker compose -f docker-compose.stack.yml up -d --build
```

- **App UI:** http://localhost:8081
- **Backend API:** http://localhost:8080
- **MariaDB:** localhost:3306 (from base compose or stack)

The frontend container proxies `/api/*` to the backend service, so the SPA uses same-origin requests (empty `VITE_API_BASE_URL`).

### Production notes

1. Set `SPRING_PROFILES_ACTIVE=prod` in stack env
2. Set strong `JWT_SECRET` (32+ chars)
3. Set `OWNER_SEED_ENABLED=false` and `DEMO_SEED_ENABLED=false` after initial setup
4. Restrict `/actuator/prometheus` to your monitoring network (firewall or reverse proxy)
5. Set `CORS_ALLOWED_ORIGINS` to your public frontend URL when not using nginx proxy

## Related

- [Production readiness](./production-readiness.md)
- Root `README.md` — environment variables
