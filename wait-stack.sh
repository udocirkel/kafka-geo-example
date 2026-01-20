#!/usr/bin/env bash
set -euo pipefail

if [ "$#" -eq 0 ]; then
  echo "Usage: $0 <service> [<service> ...]"
  echo "Example: $0 db api redis"
  exit 1
fi

for svc in "$@"; do
  echo "⏳ Waiting for $svc to be healthy..."

  cid="$(docker compose ps -q "$svc")"

  if [ -z "$cid" ]; then
    echo "❌ Service '$svc' is not running"
    exit 2
  fi

  while true; do
    status="$(docker inspect --format='{{.State.Health.Status}}' "$cid" 2>/dev/null || echo "none")"

    if [ "$status" = "healthy" ]; then
      echo "✅ $svc is healthy"
      break
    fi

    if [ "$status" = "unhealthy" ]; then
      echo "❌ $svc is unhealthy"
      docker inspect "$cid" --format='{{json .State.Health.Log}}' | jq .
      exit 3
    fi

    if [ "$status" = "none" ]; then
      echo "❌ $svc has no healthcheck"
      exit 4
    fi

    sleep 2
  done
done

echo "🎉 All requested services are healthy."