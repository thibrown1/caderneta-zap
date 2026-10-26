#!/usr/bin/env bash
# Baixa a versão nova do GitHub e reinicia: bash deploy/atualizar.sh
set -euo pipefail
cd "$(dirname "$0")/.."
git pull --ff-only
docker compose up -d --build
docker image prune -f >/dev/null
docker compose ps
