# Production readiness — Phase 22

## Purpose

Harden the backend for real deployment: health probes for orchestrators, security headers, production profile defaults, and fail-fast validation of critical secrets.

## Actuator endpoints

Spring Boot Actuator is enabled with a minimal public surface:

| Endpoint | Auth | Purpose |
|----------|------|---------|
| `/actuator/health/liveness` | Public | Process is running (Kubernetes liveness) |
| `/actuator/health/readiness` | Public | App ready to serve traffic (DB connectivity) |
| `/actuator/info` | Public | Application name and version |
| `/actuator/prometheus` | Public (restrict in prod) | Prometheus scrape metrics |

The existing `/api/health` endpoint remains for the frontend and returns the MDL `ApiResponse` JSON shape.

### Correlation IDs (Phase 43)

All requests log with `correlationId` in MDC and return `X-Correlation-Id` response header. See [observability-deploy.md](./observability-deploy.md).

Other actuator endpoints are not exposed beyond health, info, and prometheus.

## Security headers

All API responses include:

- `X-Content-Type-Options: nosniff`
- `X-Frame-Options: DENY`
- `Referrer-Policy: strict-origin-when-cross-origin`

## Production profile

Activate with `SPRING_PROFILES_ACTIVE=prod`.

| Setting | Production default |
|---------|-------------------|
| `OWNER_SEED_ENABLED` | `false` |
| `DEMO_SEED_ENABLED` | `false` |
| SQL formatting | disabled |
| Root log level | `WARN` |

### Startup validation (`prod` only)

`ProductionStartupValidator` fails startup if:

- `JWT_SECRET` is missing, shorter than 32 characters, uses the dev default, or looks like a placeholder (`change_me`, etc.)
- `OWNER_SEED_ENABLED` or `DEMO_SEED_ENABLED` is true (Phase 39+)

## Docker full stack (Phase 43)

See [observability-deploy.md](./observability-deploy.md) for `docker compose -f docker-compose.stack.yml up -d --build`.

## Deploy checklist

1. Set `SPRING_PROFILES_ACTIVE=prod`
2. Set a strong `JWT_SECRET` (32+ random characters)
3. Set `DB_*` credentials and `CORS_ALLOWED_ORIGINS` to your frontend URL(s)
4. Set `OWNER_SEED_ENABLED=false` and `DEMO_SEED_ENABLED=false` after initial setup
5. Point load balancer health checks to `/actuator/health/readiness`
6. Keep `/api/health` for application-level status in the UI

## Related

- Root `README.md` — environment variables
- `.env.example` — local configuration template
