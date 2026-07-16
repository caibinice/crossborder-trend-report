#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
LOG_DIR="$REPO_ROOT/logs"
STATE_FILE="$LOG_DIR/dev-processes.json"
ENV_FILE="${1:-$REPO_ROOT/.env}"
SKIP_INSTALL="${SKIP_INSTALL:-0}"
CREDENTIALS_FILE="$REPO_ROOT/credentials.txt"

mkdir -p "$LOG_DIR"

load_env() {
  local file="$1"
  [[ -f "$file" ]] || return 0
  while IFS= read -r line || [[ -n "$line" ]]; do
    line="${line#"${line%%[![:space:]]*}"}"
    [[ -z "$line" || "${line:0:1}" == "#" ]] && continue
    if [[ "$line" == *=* ]]; then
      local key="${line%%=*}"
      local value="${line#*=}"
      value="${value%\"}"
      value="${value#\"}"
      value="${value%\'}"
      value="${value#\'}"
      export "$key=$value"
    fi
  done < "$file"
}

ini_get() {
  local file="$1"
  local section="$2"
  local key="$3"
  python3 - "$file" "$section" "$key" <<'PY'
import configparser, sys
cfg = configparser.ConfigParser()
cfg.read(sys.argv[1], encoding='utf-8')
print(cfg.get(sys.argv[2], sys.argv[3], fallback=''))
PY
}

set_default() {
  local key="$1"
  local value="$2"
  if [[ -z "${!key:-}" ]]; then
    export "$key=$value"
  fi
}

port_pid() {
  local port="$1"
  ss -ltnp "( sport = :$port )" 2>/dev/null | grep -oP 'pid=\K\d+' | head -n1 || true
}

wait_port() {
  local port="$1"
  local timeout="$2"
  local wrapper_pid="${3:-}"
  local start_ts
  start_ts="$(date +%s)"
  while true; do
    if ss -ltn "( sport = :$port )" 2>/dev/null | grep -q ":$port"; then
      return 0
    fi
    if [[ -n "$wrapper_pid" ]] && ! kill -0 "$wrapper_pid" 2>/dev/null; then
      return 1
    fi
    if (( $(date +%s) - start_ts >= timeout )); then
      return 1
    fi
    sleep 1
  done
}

ensure_port_free() {
  local port="$1"
  if ss -ltn "( sport = :$port )" 2>/dev/null | grep -q ":$port"; then
    echo "Port $port is already in use. Run scripts/stop-dev-ubuntu.sh first." >&2
    exit 1
  fi
}

load_env "$ENV_FILE"

set_default DB_TARGET remote
set_default SERVER_PORT 8090
set_default FRONTEND_HOST 127.0.0.1
set_default FRONTEND_PORT 5174
set_default CORS_ALLOWED_ORIGINS "http://localhost:${FRONTEND_PORT},http://127.0.0.1:${FRONTEND_PORT}"

DB_SECTION="mysql.remote"
if [[ "${DB_TARGET}" == "local" ]]; then
  DB_SECTION="mysql.local"
fi

if [[ -f "$CREDENTIALS_FILE" ]]; then
  remote_host="$(ini_get "$CREDENTIALS_FILE" "$DB_SECTION" host)"
  remote_port="$(ini_get "$CREDENTIALS_FILE" "$DB_SECTION" port)"
  remote_database="$(ini_get "$CREDENTIALS_FILE" "$DB_SECTION" database)"
  remote_user="$(ini_get "$CREDENTIALS_FILE" "$DB_SECTION" user)"
  remote_password="$(ini_get "$CREDENTIALS_FILE" "$DB_SECTION" password)"
  [[ -n "$remote_host" ]] && set_default MYSQL_HOST "$remote_host"
  [[ -n "$remote_port" ]] && set_default MYSQL_PORT "$remote_port"
  [[ -n "$remote_database" ]] && set_default MYSQL_DATABASE "$remote_database"
  [[ -n "$remote_user" ]] && set_default MYSQL_USER "$remote_user"
  [[ -n "$remote_password" ]] && set_default MYSQL_PASSWORD "$remote_password"
fi

if [[ "${DB_TARGET}" == "local" ]]; then
  set_default MYSQL_HOST 127.0.0.1
  set_default MYSQL_PORT 3306
  set_default MYSQL_DATABASE crossborder_trend_demo
  set_default MYSQL_USER root
  set_default MYSQL_PASSWORD ""
else
  set_default MYSQL_HOST 101.132.78.217
  set_default MYSQL_PORT 3306
  set_default MYSQL_DATABASE crossborder_trend_demo
  set_default MYSQL_USER cross_demo
  set_default MYSQL_PASSWORD ""
fi

ensure_port_free "$SERVER_PORT"
ensure_port_free "$FRONTEND_PORT"

if [[ "$SKIP_INSTALL" != "1" && ! -d "$REPO_ROOT/frontend/node_modules" ]]; then
  (cd "$REPO_ROOT/frontend" && npm ci)
fi

: > "$LOG_DIR/backend.log"
: > "$LOG_DIR/backend.err.log"
: > "$LOG_DIR/frontend.log"
: > "$LOG_DIR/frontend.err.log"

(
  cd "$REPO_ROOT/backend"
  export MYSQL_HOST MYSQL_PORT MYSQL_DATABASE MYSQL_USER MYSQL_PASSWORD SERVER_PORT CORS_ALLOWED_ORIGINS
  exec mvn spring-boot:run
) >"$LOG_DIR/backend.log" 2>"$LOG_DIR/backend.err.log" &
BACKEND_WRAPPER_PID=$!

if ! wait_port "$SERVER_PORT" 90 "$BACKEND_WRAPPER_PID"; then
  echo "Backend did not start within 90 seconds. Check $LOG_DIR/backend.log and $LOG_DIR/backend.err.log" >&2
  exit 1
fi

(
  cd "$REPO_ROOT/frontend"
  exec npm run dev -- --host "$FRONTEND_HOST" --port "$FRONTEND_PORT"
) >"$LOG_DIR/frontend.log" 2>"$LOG_DIR/frontend.err.log" &
FRONTEND_WRAPPER_PID=$!

if ! wait_port "$FRONTEND_PORT" 60 "$FRONTEND_WRAPPER_PID"; then
  echo "Frontend did not start within 60 seconds. Check $LOG_DIR/frontend.log and $LOG_DIR/frontend.err.log" >&2
  exit 1
fi

BACKEND_PID="$(port_pid "$SERVER_PORT")"
FRONTEND_PID="$(port_pid "$FRONTEND_PORT")"

cat > "$STATE_FILE" <<EOF
{
  "startedAt": "$(date '+%Y-%m-%dT%H:%M:%S')",
  "repoRoot": "$REPO_ROOT",
  "backend": {
    "wrapperPid": $BACKEND_WRAPPER_PID,
    "servicePid": ${BACKEND_PID:-0},
    "port": $SERVER_PORT,
    "url": "http://localhost:$SERVER_PORT",
    "log": "$LOG_DIR/backend.log",
    "errLog": "$LOG_DIR/backend.err.log"
  },
  "frontend": {
    "wrapperPid": $FRONTEND_WRAPPER_PID,
    "servicePid": ${FRONTEND_PID:-0},
    "port": $FRONTEND_PORT,
    "url": "http://$FRONTEND_HOST:$FRONTEND_PORT",
    "log": "$LOG_DIR/frontend.log",
    "errLog": "$LOG_DIR/frontend.err.log"
  }
}
EOF

echo "Backend:  http://localhost:$SERVER_PORT (PID: ${BACKEND_PID:-unknown})"
echo "Frontend: http://$FRONTEND_HOST:$FRONTEND_PORT (PID: ${FRONTEND_PID:-unknown})"
echo "State file: $STATE_FILE"
