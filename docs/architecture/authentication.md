# Authentication — Phase 3

## Overview

MDL Platform uses **JWT access tokens** + **refresh tokens** with server-side session tracking.

```
Login → validate password → create session → return tokens
Request → Bearer access token → JwtAuthenticationFilter → UserContext
Refresh → validate refresh token hash in DB → new access token
Logout → revoke session in DB
```

## Endpoints

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/api/auth/login` | Public | Login with email/username + password |
| POST | `/api/auth/refresh` | Public | Get new access token using refresh token |
| POST | `/api/auth/logout` | Public* | Revoke refresh token session |
| GET | `/api/auth/me` | Required | Current user profile |

*Logout accepts refresh token in body; no access token required.

## Default MDL owner account

Created automatically on first startup by `OwnerAccountSeeder`:

| Field | Default |
|-------|---------|
| Email | `owner@mdl.local` |
| Username | `owner` |
| Password | `Owner@123!` (override via `OWNER_PASSWORD` env var) |
| Role | OWNER |
| Business | MDL (GHS) |

**Change the password after first login in production.**

## JWT access token claims

- `sub` — user ID
- `email`, `username`
- `businessId`, `businessCode`, `currencyCode`
- `roles`, `permissions`
- `sessionId`

Access tokens expire in **15 minutes** (configurable).

Refresh tokens expire in **7 days** and are stored as **SHA-256 hashes** in `user_sessions`.

## Security features

- BCrypt password hashing (strength 12)
- Account lockout after 5 failed attempts (15 minutes)
- Session revocation on logout
- Stateless API — no server-side HTTP sessions
- MFA-ready (`mfa_credentials` table — implemented in a later phase)

## Environment variables

| Variable | Purpose |
|----------|---------|
| `JWT_SECRET` | Signing key (min 32 characters) |
| `JWT_ACCESS_TOKEN_EXPIRY_MINUTES` | Access token lifetime |
| `JWT_REFRESH_TOKEN_EXPIRY_DAYS` | Refresh token lifetime |
| `OWNER_PASSWORD` | Initial owner password |
| `OWNER_SEED_ENABLED` | Set `false` to disable owner seeding |

## Example login

```http
POST /api/auth/login
Content-Type: application/json

{
  "login": "owner@mdl.local",
  "password": "Owner@123!"
}
```

```json
{
  "success": true,
  "message": "Login successful",
  "data": {
    "accessToken": "eyJ...",
    "refreshToken": "uuid.uuid",
    "tokenType": "Bearer",
    "expiresInMinutes": 15,
    "user": {
      "email": "owner@mdl.local",
      "businessCode": "MDL",
      "currencyCode": "GHS",
      "roles": ["OWNER"]
    }
  }
}
```

## Using the token

```http
GET /api/auth/me
Authorization: Bearer eyJ...
```
