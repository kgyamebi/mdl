#!/usr/bin/env bash
# Disk space alert for the VPS host or Docker volume mount.
# Usage:
#   ./scripts/monitor/disk-space-check.sh
#   DISK_PATH=/ DISK_WARN_PERCENT=85 DISK_CRIT_PERCENT=92 ./scripts/monitor/disk-space-check.sh
set -euo pipefail

DISK_PATH="${DISK_PATH:-/}"
DISK_WARN_PERCENT="${DISK_WARN_PERCENT:-85}"
DISK_CRIT_PERCENT="${DISK_CRIT_PERCENT:-92}"
ALERT_WEBHOOK_URL="${ALERT_WEBHOOK_URL:-}"

usage_percent="$(df -P "${DISK_PATH}" | awk 'NR==2 {gsub(/%/,"",$5); print $5}')"
available="$(df -hP "${DISK_PATH}" | awk 'NR==2 {print $4}')"

if [[ "${usage_percent}" -ge "${DISK_CRIT_PERCENT}" ]]; then
  level="CRITICAL"
  exit_code=2
elif [[ "${usage_percent}" -ge "${DISK_WARN_PERCENT}" ]]; then
  level="WARNING"
  exit_code=1
else
  echo "Disk OK: ${DISK_PATH} ${usage_percent}% used (${available} free)"
  exit 0
fi

message="MDL disk ${level}: ${DISK_PATH} ${usage_percent}% used (${available} free)"
echo "${message}" >&2

if [[ -n "${ALERT_WEBHOOK_URL}" ]]; then
  curl -fsS -X POST -H 'Content-Type: application/json' \
    -d "{\"text\":\"${message}\"}" "${ALERT_WEBHOOK_URL}" >/dev/null || true
fi

exit "${exit_code}"
