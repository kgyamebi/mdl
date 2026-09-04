#!/usr/bin/env bash
# Daily MariaDB backup for MDL Platform (docker-compose.prod.yml).
# Usage:
#   ./scripts/backup/mariadb-backup.sh
#   BACKUP_DIR=/var/backups/mdl RETENTION_DAYS=14 ./scripts/backup/mariadb-backup.sh
set -euo pipefail

COMPOSE_FILE="${COMPOSE_FILE:-docker-compose.prod.yml}"
BACKUP_DIR="${BACKUP_DIR:-./backups/mariadb}"
RETENTION_DAYS="${RETENTION_DAYS:-14}"
DB_NAME="${DB_NAME:-mdl_platform}"
CONTAINER="${MARIADB_CONTAINER:-mdl-mariadb}"
TIMESTAMP="$(date -u +%Y%m%dT%H%M%SZ)"
ARCHIVE="${BACKUP_DIR}/${DB_NAME}-${TIMESTAMP}.sql.gz"

mkdir -p "${BACKUP_DIR}"

if ! docker ps --format '{{.Names}}' | grep -qx "${CONTAINER}"; then
  echo "MariaDB container '${CONTAINER}' is not running" >&2
  exit 1
fi

echo "Creating backup ${ARCHIVE}"
docker exec "${CONTAINER}" mariadb-dump \
  --single-transaction \
  --routines \
  --triggers \
  --databases "${DB_NAME}" \
  | gzip > "${ARCHIVE}"

echo "Backup complete ($(du -h "${ARCHIVE}" | awk '{print $1}'))"

find "${BACKUP_DIR}" -name "${DB_NAME}-*.sql.gz" -mtime +"${RETENTION_DAYS}" -delete
echo "Pruned backups older than ${RETENTION_DAYS} days"
