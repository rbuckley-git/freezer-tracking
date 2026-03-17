#!/usr/bin/env bash
set -euo pipefail

CLUSTER="freezer-tracking-cluster"
API_SERVICE="freezer-tracking-api"
WEB_SERVICE="freezer-tracking-web"
DOWN_COUNT=0
UP_COUNT=1
WAIT_TIMEOUT_SECONDS=600
POLL_SECONDS=5

usage() {
  cat <<'EOF'
Usage: scripts/ecs-bounce-services.sh [options]

Hard bounce two ECS services by scaling both down, waiting for zero running/pending,
then scaling both up.

Options:
  --cluster <name>         ECS cluster name (default: freezer-tracking-cluster)
  --api-service <name>     API service name (default: freezer-tracking-api)
  --web-service <name>     Web service name (default: freezer-tracking-web)
  --down <count>           Down desired count (default: 0)
  --up <count>             Up desired count (default: 1)
  --timeout <seconds>      Wait timeout in seconds per phase (default: 600)
  -h, --help               Show this help
EOF
}

require_command() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "Required command not found: $1" >&2
    exit 1
  fi
}

wait_for_state() {
  local service="$1"
  local desired="$2"
  local deadline=$((SECONDS + WAIT_TIMEOUT_SECONDS))

  while true; do
    local counts
    counts="$(aws ecs describe-services \
      --cluster "$CLUSTER" \
      --services "$service" \
      --query 'services[0].[runningCount,pendingCount,desiredCount]' \
      --output text)"

    local running pending current_desired
    running="$(echo "$counts" | awk '{print $1}')"
    pending="$(echo "$counts" | awk '{print $2}')"
    current_desired="$(echo "$counts" | awk '{print $3}')"

    echo "service=$service desired=$current_desired running=$running pending=$pending"

    if [[ "$running" == "$desired" && "$pending" == "0" && "$current_desired" == "$desired" ]]; then
      return 0
    fi

    if (( SECONDS >= deadline )); then
      echo "Timed out waiting for $service to reach desired=$desired" >&2
      exit 1
    fi

    sleep "$POLL_SECONDS"
  done
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --cluster)
      CLUSTER="$2"
      shift 2
      ;;
    --api-service)
      API_SERVICE="$2"
      shift 2
      ;;
    --web-service)
      WEB_SERVICE="$2"
      shift 2
      ;;
    --down)
      DOWN_COUNT="$2"
      shift 2
      ;;
    --up)
      UP_COUNT="$2"
      shift 2
      ;;
    --timeout)
      WAIT_TIMEOUT_SECONDS="$2"
      shift 2
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "Unknown argument: $1" >&2
      usage
      exit 1
      ;;
  esac
done

require_command aws

echo "Scaling down services to $DOWN_COUNT..."
aws ecs update-service --cluster "$CLUSTER" --service "$WEB_SERVICE" --desired-count "$DOWN_COUNT" >/dev/null
aws ecs update-service --cluster "$CLUSTER" --service "$API_SERVICE" --desired-count "$DOWN_COUNT" >/dev/null

wait_for_state "$WEB_SERVICE" "$DOWN_COUNT"
wait_for_state "$API_SERVICE" "$DOWN_COUNT"

echo "Scaling up services to $UP_COUNT..."
aws ecs update-service --cluster "$CLUSTER" --service "$API_SERVICE" --desired-count "$UP_COUNT" >/dev/null
aws ecs update-service --cluster "$CLUSTER" --service "$WEB_SERVICE" --desired-count "$UP_COUNT" >/dev/null

wait_for_state "$API_SERVICE" "$UP_COUNT"
wait_for_state "$WEB_SERVICE" "$UP_COUNT"

echo "Bounce complete."
