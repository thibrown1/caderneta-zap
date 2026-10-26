#!/usr/bin/env bash
# Prepara uma VM Ubuntu da Oracle Cloud: swap, Docker e portas 80/443 liberadas no firewall da própria VM.
# Rode uma vez: bash deploy/preparar-vm.sh
set -euo pipefail

if ! swapon --show | grep -q .; then
  echo ">> Criando 2 GB de swap (ajuda muito na VM de 1 GB)"
  sudo fallocate -l 2G /swapfile
  sudo chmod 600 /swapfile
  sudo mkswap /swapfile
  sudo swapon /swapfile
  echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab >/dev/null
fi

if ! command -v docker >/dev/null 2>&1; then
  echo ">> Instalando Docker"
  curl -fsSL https://get.docker.com | sudo sh
  sudo usermod -aG docker "$USER"
fi

echo ">> Liberando as portas 80 e 443 (as imagens Ubuntu da Oracle bloqueiam por padrão)"
for porta in 80 443; do
  if ! sudo iptables -C INPUT -p tcp -m state --state NEW --dport "$porta" -j ACCEPT 2>/dev/null; then
    sudo iptables -I INPUT 6 -p tcp -m state --state NEW --dport "$porta" -j ACCEPT
  fi
done
if command -v netfilter-persistent >/dev/null 2>&1; then
  sudo netfilter-persistent save
fi

echo ">> Pronto. Saia do SSH e entre de novo para usar o docker sem sudo."
