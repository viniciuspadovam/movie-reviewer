#!/usr/bin/env bash
# Daily logical backup of the production database to Cloudflare R2.
# Cron (as the deploy user): 15 4 * * * /opt/movie-review/backup.sh >> /var/log/movie-review-backup.log 2>&1
set -euo pipefail

cd "$(dirname "$0")"
set -a
source .env
set +a

timestamp="$(date -u +%Y-%m-%dT%H-%M-%SZ)"
target="${BACKUP_REMOTE}/moviereview-${timestamp}.dump"

echo "[$(date -u +%FT%TZ)] Iniciando backup para ${target}"
docker compose exec -T postgres pg_dump -U "${POSTGRES_USER}" -d "${POSTGRES_DB}" --format=custom \
  | rclone rcat "${target}"

size="$(rclone size --json "${target}" | sed -E 's/.*"bytes":([0-9]+).*/\1/')"
if [ "${size}" -lt 1024 ]; then
  echo "[$(date -u +%FT%TZ)] ERRO: backup suspeito, apenas ${size} bytes" >&2
  exit 1
fi

rclone delete --min-age "${BACKUP_RETENTION_DAYS}d" "${BACKUP_REMOTE}"
echo "[$(date -u +%FT%TZ)] Backup concluído (${size} bytes); arquivos com mais de ${BACKUP_RETENTION_DAYS} dias removidos"
