from __future__ import annotations

import hashlib
import json
import os
import re
import secrets
import shlex
import subprocess
import tarfile
from datetime import UTC, datetime
from pathlib import Path

from remote_client import RemoteClient, STATE_DIR, read_credentials

ROOT = Path(__file__).resolve().parents[2]
APP_ROOT = "/opt/crossborder-trend-report"


def git_revision() -> str:
    revision = subprocess.check_output(
        ["git", "rev-parse", "--short", "HEAD"],
        cwd=ROOT,
        text=True,
    ).strip()
    dirty = subprocess.check_output(
        ["git", "status", "--porcelain", "--untracked-files=no"],
        cwd=ROOT,
        text=True,
    ).strip()
    return f"{revision}-working" if dirty else revision


def find_jar() -> Path:
    candidates = [
        path
        for path in (ROOT / "backend" / "target").glob("*.jar")
        if not path.name.endswith(".jar.original")
    ]
    if len(candidates) != 1:
        raise RuntimeError("Expected one executable JAR in backend/target.")
    return candidates[0]


def build_archive(destination: Path, jar: Path) -> None:
    dist = ROOT / "frontend" / "dist"
    if not (dist / "index.html").exists():
        raise RuntimeError("Missing frontend/dist. Run scripts/deploy.ps1.")
    destination.parent.mkdir(parents=True, exist_ok=True)
    with tarfile.open(destination, "w:gz") as bundle:
        for source in sorted(dist.rglob("*")):
            if source.is_file():
                bundle.add(
                    source,
                    arcname=(
                        Path("dist") / source.relative_to(dist)
                    ).as_posix(),
                )
        bundle.add(jar, arcname="app.jar")


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def latest_migration_version() -> int:
    versions = []
    for path in (
        ROOT / "backend" / "src" / "main" / "resources" / "db" / "migration"
    ).glob("V*__*.sql"):
        match = re.fullmatch(r"V(\d+)__.+\.sql", path.name)
        if match:
            versions.append(int(match.group(1)))
    if not versions:
        raise RuntimeError("No Flyway migrations were found.")
    return max(versions)


def systemd_env(values: dict[str, str]) -> bytes:
    return (
        "\n".join(
            f"{key}={json.dumps(str(value), ensure_ascii=False)}"
            for key, value in values.items()
        )
        + "\n"
    ).encode("utf-8")


def load_action_auth(credentials) -> dict[str, str]:
    path = STATE_DIR / "action-auth.json"
    configured_password = (
        os.environ.get("AI_PLATFORM_ACTION_PASSWORD", "")
        or credentials.get(
            "platform.action",
            "password",
            fallback="",
        )
    )
    if path.exists():
        value = json.loads(path.read_text(encoding="utf-8"))
        if configured_password:
            value["password"] = configured_password
    else:
        value = {
            "password": configured_password,
            "tokenSecret": secrets.token_urlsafe(48),
        }
    if not value.get("tokenSecret"):
        raise RuntimeError("Local action-auth.json is incomplete.")
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(
        json.dumps(value, ensure_ascii=False, indent=2),
        encoding="utf-8",
    )
    return value


def load_jwt_secret() -> str:
    path = STATE_DIR / "deploy-secrets.json"
    if path.exists():
        value = json.loads(path.read_text(encoding="utf-8"))
    else:
        value = {"jwtSecret": secrets.token_urlsafe(48)}
    if not value.get("jwtSecret"):
        raise RuntimeError("Local deploy-secrets.json is incomplete.")
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(
        json.dumps(value, ensure_ascii=False, indent=2),
        encoding="utf-8",
    )
    return value["jwtSecret"]


def optional_section(credentials, name: str):
    return credentials[name] if credentials.has_section(name) else {}


def environment(
    credentials,
    action_auth: dict[str, str],
    jwt_secret: str,
    public_host: str,
) -> bytes:
    mysql = credentials["mysql.remote"]
    llm = credentials["deepseek.api"]
    rakuten = optional_section(credentials, "rakuten.api")
    yahoo = optional_section(credentials, "yahoo.shopping")
    rainforest = optional_section(credentials, "rainforest.api")
    apify = optional_section(credentials, "apify.api")
    values = {
        "SERVER_ADDRESS": "127.0.0.1",
        "SERVER_PORT": "8090",
        "MYSQL_HOST": "127.0.0.1",
        "MYSQL_PORT": mysql.get("port", "3306"),
        "MYSQL_DATABASE": mysql["database"],
        "MYSQL_USER": mysql["user"],
        "MYSQL_PASSWORD": mysql["password"],
        "DB_POOL_MAX_SIZE": "3",
        "DB_POOL_MIN_IDLE": "0",
        "TOMCAT_MAX_THREADS": "20",
        "TOMCAT_MIN_SPARE_THREADS": "2",
        "TOMCAT_MAX_CONNECTIONS": "40",
        "TOMCAT_ACCEPT_COUNT": "20",
        "AUTH_ENABLED": "true",
        "JWT_SECRET": jwt_secret,
        "CORS_ALLOWED_ORIGINS": f"https://{public_host},https://www.{public_host}",
        "REPORT_CRON": "0 30 8 * * *",
        "SOURCE_MODE": "external",
        "AI_ENRICHMENT_ENABLED": "true",
        "DEEPSEEK_BASE_URL": llm.get(
            "base-url",
            "https://api.deepseek.com",
        ),
        "DEEPSEEK_API_KEY": llm.get("api-key", llm.get("token", "")),
        "DEEPSEEK_MODEL": llm.get("model", "deepseek-v4-flash"),
        "DEEPSEEK_THINKING_ENABLED": "true",
        "DEEPSEEK_REASONING_EFFORT": "max",
        "RAKUTEN_APPLICATION_ID": rakuten.get("application_id", ""),
        "RAKUTEN_ACCESS_KEY": rakuten.get("access_key", ""),
        "RAKUTEN_AFFILIATE_ID": rakuten.get("affiliate_id", ""),
        "RAKUTEN_API_VERSION": rakuten.get("api_version", "20260701"),
        "RAKUTEN_API_BASE_URL": rakuten.get(
            "api_base_url",
            "https://api-gateway-prod.gslb.rdcnw.net",
        ),
        "YAHOO_SHOPPING_CLIENT_ID": yahoo.get("client_id", ""),
        "RAINFOREST_API_KEY": rainforest.get("api_key", ""),
        "APIFY_TOKEN": apify.get("api_token", ""),
    }
    if action_auth.get("password"):
        values["INITIAL_ADMIN_PASSWORD"] = action_auth["password"]
        values["FIXED_ADMIN_PASSWORD"] = action_auth["password"]
    return systemd_env(values)


def mysql_client_config(credentials) -> bytes:
    mysql = credentials["mysql.remote"]
    values = {
        "host": mysql["host"],
        "port": mysql.get("port", "3306"),
        "user": mysql["user"],
        "password": mysql["password"],
        "default-character-set": mysql.get("charset", "utf8mb4"),
    }
    if any("\n" in value or "\r" in value for value in values.values()):
        raise RuntimeError("MySQL credentials contain an invalid newline.")
    return (
        "[client]\n"
        + "\n".join(f"{key}={value}" for key, value in values.items())
        + "\n"
    ).encode("utf-8")


def render_service() -> bytes:
    template = (
        ROOT
        / "deploy"
        / "systemd"
        / "crossborder-trend.service.template"
    ).read_text(encoding="utf-8")
    return template.replace("__APP_USER__", "aiapps").encode("utf-8")


def main() -> None:
    credentials = read_credentials()
    action_auth = load_action_auth(credentials)
    jwt_secret = load_jwt_secret()
    release = f"{datetime.now(UTC):%Y%m%d%H%M%S}-{git_revision()}"
    archive = STATE_DIR / f"crossborder-{release}.tar.gz"
    remote_archive = f"/tmp/{archive.name}"
    remote_mysql = f"/tmp/.crossborder-mysql-{release}.cnf"
    build_archive(archive, find_jar())
    archive_sha256 = sha256(archive)
    expected_flyway_version = latest_migration_version()

    remote = RemoteClient()
    try:
        public_host = remote.config["host"]
        remote.upload_file(archive, remote_archive, 0o600)
        remote.upload_bytes(
            environment(
                credentials,
                action_auth,
                jwt_secret,
                public_host,
            ),
            "/tmp/crossborder-app.env",
            0o600,
        )
        remote.upload_bytes(
            render_service(),
            "/tmp/crossborder-trend.service",
            0o644,
        )
        remote.upload_bytes(
            mysql_client_config(credentials),
            remote_mysql,
            0o600,
        )
        database = shlex.quote(credentials["mysql.remote"]["database"])
        wrapper = f"""#!/usr/bin/env bash
set -Eeuo pipefail

root={shlex.quote(APP_ROOT)}
release={shlex.quote(release)}
archive={shlex.quote(remote_archive)}
expected_archive_sha={shlex.quote(archive_sha256)}
mysql_cnf={shlex.quote(remote_mysql)}
database={database}
expected_flyway_version={expected_flyway_version}
incoming_env=/tmp/crossborder-app.env
incoming_service=/tmp/crossborder-trend.service
merged_env=/tmp/crossborder-app.env.merged
backup_dir="$root/shared/backups"
db_backup="$backup_dir/pre-deploy-$release.sql.gz"
env_backup="$backup_dir/app.env-$release.backup"
service_backup="$backup_dir/crossborder-trend.service-$release.backup"
previous="$(readlink -f "$root/current" 2>/dev/null || true)"
previous_web="$(readlink -f "$root/www/crossBorderTrend" 2>/dev/null || true)"
before_ai_blog="$(readlink -f /opt/ai-blog/current 2>/dev/null || true)"
before_nginx_pid="$(systemctl show nginx -p MainPID --value || true)"
before_quant_pid="$(systemctl show ai-quant-api -p MainPID --value || true)"
before_quant_worker_pid="$(systemctl show ai-quant-worker -p MainPID --value || true)"
before_cockpit_pid="$(systemctl show enterprise-ai-cockpit -p MainPID --value || true)"
before_nginx_conf_sha="$(sha256sum /etc/nginx/conf.d/ai-platform.conf | awk '{{print $1}}')"
had_env=0
had_service=0
modified=0

cleanup() {{
  rm -f "$archive" "$mysql_cnf" "$incoming_env" "$incoming_service" \
    "$merged_env" /tmp/deploy-crossborder-trend.sh
}}

rollback() {{
  status=$?
  trap - ERR EXIT
  if [[ "$modified" == 1 ]]; then
    if [[ "$had_env" == 1 ]]; then
      install -o aiapps -g aiapps -m 600 "$env_backup" "$root/shared/app.env"
    else
      rm -f "$root/shared/app.env"
    fi
    if [[ "$had_service" == 1 ]]; then
      install -m 644 "$service_backup" /etc/systemd/system/crossborder-trend.service
    else
      rm -f /etc/systemd/system/crossborder-trend.service
    fi
    if [[ -n "$previous" && -d "$previous" ]]; then
      ln -sfn "$previous" "$root/current.rollback"
      mv -Tf "$root/current.rollback" "$root/current"
    fi
    if [[ -n "$previous_web" && -d "$previous_web" ]]; then
      ln -sfn "$previous_web" "$root/www/crossBorderTrend.rollback"
      mv -Tf "$root/www/crossBorderTrend.rollback" "$root/www/crossBorderTrend"
    fi
    systemctl daemon-reload || true
    systemctl restart crossborder-trend.service || true
  fi
  cleanup
  printf 'ROLLBACK_STATUS=%s\n' "$status"
  exit "$status"
}}
trap rollback ERR
trap cleanup EXIT

test "$(sha256sum "$archive" | awk '{{print $1}}')" = "$expected_archive_sha"

if [[ ! -x /usr/local/bin/java17 ]]; then
  dnf -y --disablerepo='epel*' install java-17-openjdk-headless >/dev/null
  java17="$(find /usr/lib/jvm -type f -path '*java-17*/bin/java' | head -n 1)"
  test -x "$java17"
  ln -sfn "$java17" /usr/local/bin/java17
fi
id aiapps >/dev/null 2>&1 ||   useradd --system --home-dir /opt/ai-platform --shell /sbin/nologin aiapps

mkdir -p "$root/releases/$release" "$root/shared" "$root/www" "$backup_dir"
tar -xzf "$archive" -C "$root/releases/$release"
test -s "$root/releases/$release/app.jar"
test -s "$root/releases/$release/dist/index.html"
chown -R aiapps:aiapps "$root/releases/$release" "$root/shared"
chmod 755 "$root" "$root/releases" "$root/www" "$root/releases/$release"

if [[ -s "$root/shared/app.env" ]]; then
  had_env=1
  cp -a "$root/shared/app.env" "$env_backup"
fi
if [[ -s /etc/systemd/system/crossborder-trend.service ]]; then
  had_service=1
  cp -a /etc/systemd/system/crossborder-trend.service "$service_backup"
fi

dump_extra=()
if mysqldump --help 2>/dev/null | grep -q -- '--no-tablespaces'; then
  dump_extra+=(--no-tablespaces)
fi
mysqldump --defaults-extra-file="$mysql_cnf" "${{dump_extra[@]}}" \
  --single-transaction --quick --skip-lock-tables "$database" \
  | gzip -9 > "$db_backup"
test -s "$db_backup"

# Keep server-only tuning and preserve the existing JWT secret during the
# transition from the legacy publisher. Values generated by this repository
# remain authoritative for every other managed key.
cp "$incoming_env" "$merged_env"
if [[ "$had_env" == 1 ]]; then
  if grep -q '^JWT_SECRET=' "$root/shared/app.env"; then
    sed -i '/^JWT_SECRET=/d' "$merged_env"
  fi
  awk -F= '
    NR == FNR {{
      if ($0 ~ /^[A-Za-z_][A-Za-z0-9_]*=/) managed[$1] = 1
      next
    }}
    $0 ~ /^[A-Za-z_][A-Za-z0-9_]*=/ && !($1 in managed) {{ print }}
  ' "$merged_env" "$root/shared/app.env" >> "$merged_env"
fi
if ! grep -Eq '^FIXED_ADMIN_PASSWORD=.+$' "$merged_env"; then
  echo 'Missing platform action password for first standalone deployment.'
  false
fi

modified=1
install -o aiapps -g aiapps -m 600 "$merged_env" "$root/shared/app.env"
install -m 644 "$incoming_service" /etc/systemd/system/crossborder-trend.service

ln -sfn "$root/releases/$release" "$root/current.next"
mv -Tf "$root/current.next" "$root/current"
ln -sfn "$root/releases/$release/dist" "$root/www/crossBorderTrend.next"
mv -Tf "$root/www/crossBorderTrend.next" "$root/www/crossBorderTrend"

systemctl daemon-reload
systemctl enable crossborder-trend.service >/dev/null
systemctl restart crossborder-trend.service

healthy=false
for _attempt in $(seq 1 60); do
  if curl -fsS http://127.0.0.1:8090/api/health >/dev/null; then
    healthy=true
    break
  fi
  sleep 2
done
if [[ "$healthy" != true ]]; then
  journalctl -u crossborder-trend.service -n 80 --no-pager
  false
fi

systemctl is-active --quiet crossborder-trend.service
flyway_version="$(mysql --defaults-extra-file="$mysql_cnf" --batch \
  --skip-column-names "$database" \
  -e 'SELECT MAX(CAST(version AS UNSIGNED)) FROM flyway_schema_history WHERE success=1;')"
test "$flyway_version" = "$expected_flyway_version"
products_per_category="$(mysql --defaults-extra-file="$mysql_cnf" --batch \
  --skip-column-names "$database" \
  -e "SELECT products_per_category FROM admin_settings WHERE tenant_id='default' LIMIT 1;")"
test "$products_per_category" = 20
if command -v nginx >/dev/null 2>&1; then nginx -t; fi
test "$before_nginx_pid" =   "$(systemctl show nginx -p MainPID --value || true)"
test "$before_quant_pid" =   "$(systemctl show ai-quant-api -p MainPID --value || true)"
test "$before_quant_worker_pid" =   "$(systemctl show ai-quant-worker -p MainPID --value || true)"
test "$before_cockpit_pid" =   "$(systemctl show enterprise-ai-cockpit -p MainPID --value || true)"
test "$before_ai_blog" = "$(readlink -f /opt/ai-blog/current 2>/dev/null || true)"
test "$before_nginx_conf_sha" = "$(sha256sum /etc/nginx/conf.d/ai-platform.conf | awk '{{print $1}}')"

curl -kfsS {shlex.quote(f"https://{public_host}/")} >/dev/null
curl -kfsS {shlex.quote(f"https://{public_host}/crossBorderTrend/")} >/dev/null
curl -kfsS {shlex.quote(f"https://{public_host}/crossBorderTrend/api/health")} >/dev/null
curl -kfsS {shlex.quote(f"https://{public_host}/smartCockpit/")} >/dev/null
curl -kfsS {shlex.quote(f"https://{public_host}/quant/")} >/dev/null

find "$root/releases" -mindepth 1 -maxdepth 1 -type d -printf '%T@ %p\\n'   | sort -nr | tail -n +6 | cut -d' ' -f2- | xargs -r rm -rf
trap - ERR EXIT
cleanup

printf 'CROSSBORDER_SERVICE=%s\\n' "$(systemctl is-active crossborder-trend.service)"
printf 'PREVIOUS_RELEASE=%s\\n' "$previous"
printf 'CROSSBORDER_RELEASE_PATH=%s\\n' "$(readlink -f "$root/current")"
printf 'DB_BACKUP=%s\\n' "$db_backup"
printf 'ENV_BACKUP=%s\\n' "$env_backup"
printf 'SERVICE_BACKUP=%s\\n' "$service_backup"
printf 'AI_BLOG_RELEASE=%s\\n' "$before_ai_blog"
printf 'NGINX_PID_UNCHANGED=1\\n'
printf 'QUANT_PIDS_UNCHANGED=1\\n'
printf 'COCKPIT_PID_UNCHANGED=1\\n'
printf 'NGINX_CONFIG_UNCHANGED=1\\n'
printf 'FLYWAY_VERSION=%s\\n' "$flyway_version"
printf 'PRODUCTS_PER_CATEGORY=%s\\n' "$products_per_category"
"""
        remote.upload_bytes(
            wrapper.encode("utf-8"),
            "/tmp/deploy-crossborder-trend.sh",
            0o700,
        )
        output = remote.run(
            "/bin/bash /tmp/deploy-crossborder-trend.sh",
            root=True,
            timeout=900,
        )
        record = {
            "release": release,
            "commit": subprocess.check_output(
                ["git", "rev-parse", "HEAD"],
                cwd=ROOT,
                text=True,
            ).strip(),
            "archive_sha256": archive_sha256,
            "public_url": f"https://{public_host}/crossBorderTrend/",
            "deployed_at": datetime.now(UTC).isoformat(),
        }
        allowed = {
            "CROSSBORDER_SERVICE",
            "PREVIOUS_RELEASE",
            "CROSSBORDER_RELEASE_PATH",
            "DB_BACKUP",
            "ENV_BACKUP",
            "SERVICE_BACKUP",
            "AI_BLOG_RELEASE",
            "NGINX_PID_UNCHANGED",
            "QUANT_PIDS_UNCHANGED",
            "COCKPIT_PID_UNCHANGED",
            "NGINX_CONFIG_UNCHANGED",
            "FLYWAY_VERSION",
            "PRODUCTS_PER_CATEGORY",
        }
        for line in output.splitlines():
            if "=" in line:
                key, value = line.split("=", 1)
                if key in allowed:
                    record[key.lower()] = value.strip()
        (STATE_DIR / "last-standalone-deployment.json").write_text(
            json.dumps(record, ensure_ascii=False, indent=2),
            encoding="utf-8",
        )
    finally:
        remote.close()
        archive.unlink(missing_ok=True)

    print(f"PUBLIC_URL=https://{remote.config['host']}/crossBorderTrend/")
    print(f"CROSSBORDER_RELEASE={release}")


if __name__ == "__main__":
    main()
