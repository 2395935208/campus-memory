#!/usr/bin/env bash
# Alibaba Cloud ECS cloud-init user-data for CampusMemory.
# This file is the repository-level code evidence for the Alibaba Cloud deployment path.
set -euo pipefail

export DEBIAN_FRONTEND=noninteractive
apt-get update
apt-get install -y docker.io docker-compose-v2 git curl
systemctl enable --now docker

install -d -m 0755 /opt/campus-memory
git clone https://github.com/2395935208/campus-memory.git /opt/campus-memory/source
cd /opt/campus-memory/source

# Add DASHSCOPE_API_KEY to /opt/campus-memory/source/.env after provisioning.
# Never commit or paste a real key into cloud-init logs.
printf 'HOST_PORT=80\n' > .env
chmod 600 .env
docker compose up -d --build

for attempt in $(seq 1 30); do
  if curl --fail --silent http://127.0.0.1/api/health; then
    exit 0
  fi
  sleep 2
done
exit 1
