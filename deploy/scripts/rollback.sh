#!/bin/sh
set -eu
slot="${1:?usage: rollback.sh blue|green}"
case "$slot" in blue) percent=0 ;; green) percent=100 ;; *) echo "slot must be blue or green" >&2; exit 2;; esac
deploy/scripts/set-traffic.sh "$percent"
echo "$slot" > deploy/active-slot
echo "traffic rolled back to $slot; database migrations were intentionally not reversed"
