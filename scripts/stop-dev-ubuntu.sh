#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
STATE_FILE="$REPO_ROOT/logs/dev-processes.json"

port_pids() {
  local port="$1"
  ss -ltnp "( sport = :$port )" 2>/dev/null | grep -oP 'pid=\K\d+' | sort -u || true
}

wait_port_closed() {
  local port="$1"
  local timeout="$2"
  local start_ts
  start_ts="$(date +%s)"
  while true; do
    if ! ss -ltn "( sport = :$port )" 2>/dev/null | grep -q ":$port"; then
      return 0
    fi
    if (( $(date +%s) - start_ts >= timeout )); then
      return 1
    fi
    sleep 1
  done
}

kill_target() {
  local port="$1"
  shift
  local pids=("$@")
  while IFS= read -r pid; do
    [[ -n "$pid" ]] && pids+=("$pid")
  done < <(port_pids "$port")

  if [[ "${#pids[@]}" -gt 0 ]]; then
    printf '%s\n' "${pids[@]}" | awk 'NF' | sort -u | xargs -r kill -9
  fi

  wait_port_closed "$port" 20 || true
}

if [[ -f "$STATE_FILE" ]]; then
  BACKEND_PORT="$(grep -oP '"port":\s*\K\d+' "$STATE_FILE" | sed -n '1p')"
  FRONTEND_PORT="$(grep -oP '"port":\s*\K\d+' "$STATE_FILE" | sed -n '2p')"
  BACKEND_SERVICE_PID="$(grep -oP '"servicePid":\s*\K\d+' "$STATE_FILE" | sed -n '1p')"
  BACKEND_WRAPPER_PID="$(grep -oP '"wrapperPid":\s*\K\d+' "$STATE_FILE" | sed -n '1p')"
  FRONTEND_SERVICE_PID="$(grep -oP '"servicePid":\s*\K\d+' "$STATE_FILE" | sed -n '2p')"
  FRONTEND_WRAPPER_PID="$(grep -oP '"wrapperPid":\s*\K\d+' "$STATE_FILE" | sed -n '2p')"
else
  BACKEND_PORT=8090
  FRONTEND_PORT=5174
  BACKEND_SERVICE_PID=0
  BACKEND_WRAPPER_PID=0
  FRONTEND_SERVICE_PID=0
  FRONTEND_WRAPPER_PID=0
fi

kill_target "$BACKEND_PORT" "$BACKEND_SERVICE_PID" "$BACKEND_WRAPPER_PID"
kill_target "$FRONTEND_PORT" "$FRONTEND_SERVICE_PID" "$FRONTEND_WRAPPER_PID"

rm -f "$STATE_FILE"
echo "Stop command finished"
