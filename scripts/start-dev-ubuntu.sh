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
import sys
path, wanted_section, wanted_key = sys.argv[1:4]
section = ''
raw_value = ''
with open(path, encoding='utf-8') as stream:
    for original in stream:
        line = original.strip()
        if not line or line.startswith(('#', ';')):
            continue
        if line.startswith('[') and line.endswith(']'):
            section = line[1:-1].strip().lower()
            continue
        if section != wanted_section.lower():
            continue
        if '=' in line:
            key, value = line.split('=', 1)
            if key.strip().lower() == wanted_key.lower():
                print(value.strip().strip('"\''))
                raise SystemExit
        elif not raw_value:
            raw_value = line
if wanted_key.lower() in ('token', 'api_key', 'key'):
    print(raw_value)
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

  deepseek_key="$(ini_get "$CREDENTIALS_FILE" deepseek.api token)"
  rakuten_app_id="$(ini_get "$CREDENTIALS_FILE" rakuten.api application_id)"
  rakuten_access_key="$(ini_get "$CREDENTIALS_FILE" rakuten.api access_key)"
  rakuten_affiliate_id="$(ini_get "$CREDENTIALS_FILE" rakuten.api affiliate_id)"
  yahoo_client_id="$(ini_get "$CREDENTIALS_FILE" yahoo.shopping client_id)"
  rainforest_key="$(ini_get "$CREDENTIALS_FILE" rainforest.api api_key)"
  apify_token="$(ini_get "$CREDENTIALS_FILE" apify.api token)"
  [[ -n "$deepseek_key" ]] && set_default DEEPSEEK_API_KEY "$deepseek_key"
  [[ -n "$rakuten_app_id" ]] && set_default RAKUTEN_APPLICATION_ID "$rakuten_app_id"
  [[ -n "$rakuten_access_key" ]] && set_default RAKUTEN_ACCESS_KEY "$rakuten_access_key"
  [[ -n "$rakuten_affiliate_id" ]] && set_default RAKUTEN_AFFILIATE_ID "$rakuten_affiliate_id"
  [[ -n "$yahoo_client_id" ]] && set_default YAHOO_SHOPPING_CLIENT_ID "$yahoo_client_id"
  [[ -n "$rainforest_key" ]] && set_default RAINFOREST_API_KEY "$rainforest_key"
  [[ -n "$apify_token" ]] && set_default APIFY_TOKEN "$apify_token"
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
