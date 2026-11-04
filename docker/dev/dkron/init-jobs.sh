#!/bin/sh
set -eu

DKRON_HOST="${DKRON_HOST:-dkron}"
DKRON_PORT="${DKRON_PORT:-8998}"
DKRON_URL="http://${DKRON_HOST}:${DKRON_PORT}"
JOB_FILE="/opt/dkron/session-housekeeping-job.json"
API_KEY="${HOUSEKEEPING_API_KEY:-ordago-dev-housekeeping-secret-key-32chars}"

echo "[dkron-init] Waiting for Dkron health at ${DKRON_URL}/v1/health..."
MAX_RETRIES=30
RETRY_COUNT=0
until curl -s -f "${DKRON_URL}/v1/health" > /dev/null 2>&1; do
  RETRY_COUNT=$((RETRY_COUNT + 1))
  if [ "$RETRY_COUNT" -ge "$MAX_RETRIES" ]; then
    echo "[dkron-init] ERROR: Timed out waiting for Dkron at ${DKRON_URL}/v1/health." >&2
    exit 1
  fi
  sleep 1
done

echo "[dkron-init] Dkron is reachable. Preparing job definition from ${JOB_FILE}..."
if [ ! -f "${JOB_FILE}" ]; then
  echo "[dkron-init] ERROR: Job specification file ${JOB_FILE} not found!" >&2
  exit 1
fi

ESCAPED_KEY=$(printf '%s\n' "$API_KEY" | sed -e 's/[\\\/&|]/\\&/g')
PAYLOAD=$(sed \
  -e "s|\${HOUSEKEEPING_API_KEY:-ordago-dev-housekeeping-secret-key-32chars}|${ESCAPED_KEY}|g" \
  -e "s|\${HOUSEKEEPING_API_KEY}|${ESCAPED_KEY}|g" \
  "${JOB_FILE}")

echo "[dkron-init] Upserting job 'session-housekeeping-purge' into Dkron..."
MAX_POST_RETRIES=10
POST_RETRY=0
while [ "$POST_RETRY" -lt "$MAX_POST_RETRIES" ]; do
  POST_RETRY=$((POST_RETRY + 1))
  RESPONSE=$(curl -s -w "\nHTTP_STATUS:%{http_code}" -X POST "${DKRON_URL}/v1/jobs" \
    -H "Content-Type: application/json" \
    -d "${PAYLOAD}") || true
  STATUS=$(echo "$RESPONSE" | grep "HTTP_STATUS" | cut -d':' -f2 || echo "000")

  if [ "$STATUS" = "200" ] || [ "$STATUS" = "201" ]; then
    echo "[dkron-init] Job 'session-housekeeping-purge' registered successfully (HTTP ${STATUS})."
    exit 0
  fi

  echo "[dkron-init] Attempt ${POST_RETRY}/${MAX_POST_RETRIES} returned HTTP ${STATUS}. Retrying in 1s..."
  sleep 1
done

echo "[dkron-init] ERROR: Failed to register job after ${MAX_POST_RETRIES} attempts (Last HTTP ${STATUS})." >&2
exit 1
