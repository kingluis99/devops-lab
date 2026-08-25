#!/usr/bin/env bash
set -euo pipefail

URL="${1:-http://localhost:8080/actuator/health}"
LOG="${HOME}/healthcheck.log"

log() { printf '%s %s\n' "$(date -u '+%Y-%m-%dT%H:%M:%SZ')" "$*" >> "$LOG"; }
trap 'log "ERROR unexpected failure checking $URL"' ERR

if ! BODY="$(curl -fsS --max-time 5 "$URL" 2>>"$LOG")"; then
  log "DOWN  $URL unreachable"
  exit 1
fi

STATUS="$(printf '%s' "$BODY" | jq -r '.status // "UNKNOWN"')"

if [[ "$STATUS" == "UP" ]]; then
  log "UP    $URL"
  exit 0
else
  log "DOWN  $URL status=$STATUS"
  exit 1
fi
