#!/usr/bin/env bash
# Backup do banco, guardando os últimos 30 dias.
# Agende todo dia às 3h:  crontab -e  →  0 3 * * * /home/ubuntu/caderneta-zap/deploy/backup.sh >> /home/ubuntu/backup.log 2>&1
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p backups
arquivo="backups/caderneta-$(date +%F).sql.gz"
docker compose exec -T db pg_dump -U caderneta caderneta | gzip > "$arquivo"
find backups -name 'caderneta-*.sql.gz' -mtime +30 -delete
echo "Backup salvo em $arquivo"
