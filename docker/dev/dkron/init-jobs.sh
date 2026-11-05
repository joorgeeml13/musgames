#!/bin/sh
set -eu

DKRON_HOST="${DKRON_HOST:-dkron}"
DKRON_PORT="${DKRON_PORT:-8998}"
DKRON_URL="http://${DKRON_HOST}:${DKRON_PORT}"
API_KEY="${HOUSEKEEPING_API_KEY:-ordago-dev-housekeeping-secret-key-32chars}"

DKRON_DIR="/opt/dkron"
if [ ! -d "${DKRON_DIR}" ]; then
  DKRON_DIR="$(cd "$(dirname "$0")" && pwd)"
fi

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

echo "[dkron-init] Dkron is reachable. Registering housekeeping jobs..."

# Safely escape HOUSEKEEPING_API_KEY for embedding inside JSON-serialized string values.
# In job specs, headers is a JSON-encoded string array: quotes and backslashes require nested JSON escaping.
ESCAPED_KEY=$(API_KEY="${API_KEY}" awk '
  BEGIN {
    key = ENVIRON["API_KEY"]
    escaped = ""
    len = length(key)
    for (i = 1; i <= len; i++) {
      c = substr(key, i, 1)
      if (c == "\\") {
        escaped = escaped "\\\\\\\\"
      } else if (c == "\"") {
        escaped = escaped "\\\\\\\""
      } else {
        escaped = escaped c
      }
    }
    print escaped
  }
')

upsert_job() {
  job_file="$1"

  echo "[dkron-init] Preparing job definition from ${job_file}..."
  if [ ! -f "${job_file}" ]; then
    echo "[dkron-init] ERROR: Job specification file ${job_file} not found!" >&2
    return 1
  fi

  job_name=$(grep -o '"name"[[:space:]]*:[[:space:]]*"[^"]*"' "${job_file}" | head -n1 | cut -d'"' -f4 || echo "")
  if [ -z "${job_name}" ]; then
    job_name=$(basename "${job_file}" .json)
  fi

  # Substitute HOUSEKEEPING_API_KEY placeholders safely without regex corruption or delimiter collisions
  payload=$(ESCAPED_KEY="${ESCAPED_KEY}" awk '
    BEGIN {
      escaped = ENVIRON["ESCAPED_KEY"]
      t1 = "${HOUSEKEEPING_API_KEY:-ordago-dev-housekeeping-secret-key-32chars}"
      t2 = "${HOUSEKEEPING_API_KEY}"
    }
    {
      while ((idx = index($0, t1)) > 0) {
        $0 = substr($0, 1, idx - 1) escaped substr($0, idx + length(t1))
      }
      while ((idx = index($0, t2)) > 0) {
        $0 = substr($0, 1, idx - 1) escaped substr($0, idx + length(t2))
      }
      print
    }
  ' "${job_file}")

  echo "[dkron-init] Upserting job '${job_name}' into Dkron (REST API /v1/jobs)..."
  max_retries=10
  retry=0
  while [ "$retry" -lt "$max_retries" ]; do
    retry=$((retry + 1))
    response=$(curl -s -w "\nHTTP_STATUS:%{http_code}" -X POST "${DKRON_URL}/v1/jobs" \
      -H "Content-Type: application/json" \
      -d "${payload}") || true
    status=$(echo "$response" | grep "HTTP_STATUS" | cut -d':' -f2 || echo "000")

    if [ "$status" = "405" ] || [ "$status" = "404" ]; then
      response=$(curl -s -w "\nHTTP_STATUS:%{http_code}" -X PUT "${DKRON_URL}/v1/jobs" \
        -H "Content-Type: application/json" \
        -d "${payload}") || true
      status=$(echo "$response" | grep "HTTP_STATUS" | cut -d':' -f2 || echo "000")
    fi

    if [ "$status" = "200" ] || [ "$status" = "201" ]; then
      echo "[dkron-init] Job '${job_name}' registered successfully (HTTP ${status})."
      return 0
    fi

    echo "[dkron-init] Attempt ${retry}/${max_retries} for '${job_name}' returned HTTP ${status}. Retrying in 1s..."
    sleep 1
  done

  echo "[dkron-init] ERROR: Failed to register job '${job_name}' after ${max_retries} attempts (Last HTTP ${status})." >&2
  return 1
}

JOB_FILES="
${DKRON_DIR}/session-housekeeping-job.json
${DKRON_DIR}/guest-housekeeping-job.json
"

for file in ${JOB_FILES}; do
  if ! upsert_job "${file}"; then
    echo "[dkron-init] ERROR: Failed to register job specification: ${file}" >&2
    exit 1
  fi
done

echo "[dkron-init] All Dkron housekeeping jobs registered successfully."
exit 0
