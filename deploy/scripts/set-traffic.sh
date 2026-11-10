#!/bin/sh
set -eu
percent="${1:?usage: set-traffic.sh GREEN_PERCENT}"
case "$percent" in
  0) cp deploy/nginx/edge-blue.conf deploy/nginx/edge.conf ;;
  100) cp deploy/nginx/edge-green.conf deploy/nginx/edge.conf ;;
  *[!0-9]*|'') echo "percentage must be an integer from 0 to 100" >&2; exit 2 ;;
  *)
    if [ "$percent" -lt 1 ] || [ "$percent" -gt 99 ]; then echo "percentage must be 0..100" >&2; exit 2; fi
    sed "s/__GREEN_PERCENT__/$percent/" deploy/nginx/edge-gray.conf.template > deploy/nginx/edge.conf
    ;;
esac
docker compose -f compose.prod.yaml exec -T edge nginx -t
docker compose -f compose.prod.yaml exec -T edge nginx -s reload
echo "green traffic is now ${percent}%"
