#!/usr/bin/env bash
# Bootstrap MDL Platform on a fresh Ubuntu VPS (run as a user with docker access).
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT_DIR"

if [[ ! -f .env ]]; then
  cp .env.production.example .env
  echo "Created .env from .env.production.example — edit it before continuing."
  echo "Required: DOMAIN, LETSENCRYPT_EMAIL, DB passwords, JWT_SECRET, OWNER_PASSWORD"
  exit 1
fi

# shellcheck disable=SC1091
source .env

missing=()
for var in DOMAIN LETSENCRYPT_EMAIL DB_PASSWORD DB_ROOT_PASSWORD JWT_SECRET OWNER_PASSWORD; do
  if [[ -z "${!var:-}" ]]; then
    missing+=("$var")
  fi
done

if [[ ${#missing[@]} -gt 0 ]]; then
  echo "Missing required .env values: ${missing[*]}"
  exit 1
fi

if [[ ${#JWT_SECRET} -lt 32 ]]; then
  echo "JWT_SECRET must be at least 32 characters"
  exit 1
fi

echo "Building and starting production stack for https://${DOMAIN} ..."
docker compose -f docker-compose.prod.yml up -d --build

echo ""
echo "Done. Open https://${DOMAIN} once containers are healthy."
echo "Logs: docker compose -f docker-compose.prod.yml logs -f"
echo "After first login, set OWNER_SEED_ENABLED=false in .env and run this script again."
