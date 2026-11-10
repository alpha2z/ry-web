#!/bin/sh
set -eu
tag="${1:?usage: deploy.sh IMAGE_TAG TARGET_SLOT}"
slot="${2:?usage: deploy.sh IMAGE_TAG TARGET_SLOT}"
case "$slot" in blue|green) ;; *) echo "slot must be blue or green" >&2; exit 2;; esac

env_file=".env"
test -f "$env_file" || { echo ".env is required" >&2; exit 2; }
if [ "$slot" = green ]; then
  sed -i.bak "s/^GREEN_TAG=.*/GREEN_TAG=$tag/" "$env_file"
  stages="10 50 100"
else
  sed -i.bak "s/^BLUE_TAG=.*/BLUE_TAG=$tag/" "$env_file"
  stages="90 50 0"
fi
docker compose -f compose.prod.yaml pull "api-$slot" "web-$slot"
docker compose -f compose.prod.yaml up -d --no-deps "api-$slot"

container_id="$(docker compose -f compose.prod.yaml ps -q "api-$slot")"
attempt=0
until [ "$(docker inspect -f '{{.State.Health.Status}}' "$container_id")" = healthy ]; do
  attempt=$((attempt + 1)); [ "$attempt" -lt 60 ] || { echo "new API did not become healthy" >&2; exit 1; }; sleep 5
done
docker compose -f compose.prod.yaml up -d --no-deps "web-$slot"
docker compose -f compose.prod.yaml exec -T "web-$slot" wget -qO- http://127.0.0.1/healthz >/dev/null
docker compose -f compose.prod.yaml exec -T "web-$slot" wget -qO- http://127.0.0.1/prod-api/auth/code | grep -q '"code":200'
observation="${GRAY_OBSERVATION_SECONDS:-300}"
for green_percent in $stages; do
  deploy/scripts/set-traffic.sh "$green_percent"
  wget -qO- http://127.0.0.1/healthz >/dev/null || { deploy/scripts/rollback.sh "$([ "$slot" = green ] && echo blue || echo green)"; exit 1; }
  [ "$green_percent" = 100 ] || [ "$green_percent" = 0 ] || sleep "$observation"
done
echo "$slot" > deploy/active-slot
echo "deployed $tag to $slot"
