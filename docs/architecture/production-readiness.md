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

The existing `/api/health` endpoint remains for the frontend and returns the MDL `ApiResponse` JSON shape.

Other actuator endpoints are not exposed.

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

It logs warnings (does not fail) if demo/owner seeding is still enabled in production.

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
