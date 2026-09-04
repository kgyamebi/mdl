#!/usr/bin/env bash
# Simple uptime check for MDL Platform health endpoint.
# Usage:
#   ./scripts/monitor/health-check.sh
#   HEALTH_URL=https://app.example.com/api/health ./scripts/monitor/health-check.sh
set -euo pipefail

HEALTH_URL="${HEALTH_URL:-http://127.0.0.1/api/health}"
TIMEOUT_SECONDS="${TIMEOUT_SECONDS:-10}"
ALERT_WEBHOOK_URL="${ALERT_WEBHOOK_URL:-}"

response="$(curl -fsS --max-time "${TIMEOUT_SECONDS}" "${HEALTH_URL}" || true)"

if [[ -z "${response}" ]]; then
  message="MDL health check failed: no response from ${HEALTH_URL}"
  echo "${message}" >&2
  if [[ -n "${ALERT_WEBHOOK_URL}" ]]; then
    curl -fsS -X POST -H 'Content-Type: application/json' \
      -d "{\"text\":\"${message}\"}" "${ALERT_WEBHOOK_URL}" >/dev/null || true
  fi
  exit 1
fi

if ! grep -q '"success":true' <<<"${response}"; then
  message="MDL health check failed: unexpected payload from ${HEALTH_URL}"
  echo "${message}" >&2
  echo "${response}" >&2
  exit 1
fi

echo "MDL health check OK (${HEALTH_URL})"
