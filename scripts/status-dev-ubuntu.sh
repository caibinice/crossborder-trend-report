#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
STATE_FILE="$REPO_ROOT/logs/dev-processes.json"

show_status() {
  local name="$1"
  local port="$2"
  local pid
  pid="$(ss -ltnp "( sport = :$port )" 2>/dev/null | grep -oP 'pid=\K\d+' | head -n1 || true)"
  if [[ -z "$pid" ]]; then
    echo "$name not running (port $port is not listening)"
  else
    echo "$name running: port=$port PID=$pid"
  fi
}

if [[ -f "$STATE_FILE" ]]; then
  echo "State file: $STATE_FILE"
  cat "$STATE_FILE"
  echo
fi

show_status backend 8090
show_status frontend 5174
