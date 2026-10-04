#!/usr/bin/env bash
# Restores a backup from R2 into the running Postgres, replacing current data.
# Usage: ./restore.sh                 -> lists available backups
#        ./restore.sh <file-name>     -> restores that backup (asks for confirmation)
set -euo pipefail

cd "$(dirname "$0")"
set -a
source .env
set +a

if [ $# -eq 0 ]; then
  rclone lsl "${BACKUP_REMOTE}" | sort -k2,3
  exit 0
fi

read -r -p "Substituir os dados atuais por $1? Digite 'restaurar' para continuar: " answer
if [ "${answer}" != "restaurar" ]; then
  echo "Restauração cancelada."
  exit 1
fi

docker compose stop api
rclone cat "${BACKUP_REMOTE}/$1" \
  | docker compose exec -T postgres pg_restore -U "${POSTGRES_USER}" -d "${POSTGRES_DB}" --clean --if-exists --no-owner
docker compose start api
echo "Restauração de $1 concluída."
