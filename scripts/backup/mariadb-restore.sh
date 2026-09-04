#!/usr/bin/env bash
# Restore MariaDB from a gzipped SQL dump created by mariadb-backup.sh.
# Usage:
#   ./scripts/backup/mariadb-restore.sh ./backups/mariadb/mdl_platform-20260101T120000Z.sql.gz
#
# WARNING: overwrites the current database. Test on staging first.
set -euo pipefail

if [[ $# -ne 1 ]]; then
  echo "Usage: $0 <backup.sql.gz>" >&2
  exit 1
fi

BACKUP_FILE="$1"
DB_NAME="${DB_NAME:-mdl_platform}"
CONTAINER="${MARIADB_CONTAINER:-mdl-mariadb}"

if [[ ! -f "${BACKUP_FILE}" ]]; then
  echo "Backup file not found: ${BACKUP_FILE}" >&2
  exit 1
fi

if ! docker ps --format '{{.Names}}' | grep -qx "${CONTAINER}"; then
  echo "MariaDB container '${CONTAINER}' is not running" >&2
  exit 1
fi

echo "Restoring ${BACKUP_FILE} into ${DB_NAME} (container ${CONTAINER})"
gunzip -c "${BACKUP_FILE}" | docker exec -i "${CONTAINER}" mariadb
echo "Restore complete"
