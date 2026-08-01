from __future__ import annotations

import json
import os
import secrets
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
        if not configured_password:
            raise RuntimeError(
                "credentials.txt is missing [platform.action] password."
            )
        value = {
            "password": configured_password,
            "tokenSecret": secrets.token_urlsafe(48),
        }
    if not value.get("password") or not value.get("tokenSecret"):
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
        "INITIAL_ADMIN_PASSWORD": action_auth["password"],
        "FIXED_ADMIN_PASSWORD": action_auth["password"],
        "CORS_ALLOWED_ORIGINS": f"https://{public_host}",
        "SOURCE_MODE": "external",
        "AI_ENRICHMENT_ENABLED": "true",
        "DEEPSEEK_BASE_URL": llm.get(
            "base-url",
            "https://api.deepseek.com",
        ),
        "DEEPSEEK_API_KEY": llm.get("api-key", ""),
        "DEEPSEEK_MODEL": llm.get("model", "deepseek-v4-pro"),
        "DEEPSEEK_THINKING_ENABLED": "true",
        "DEEPSEEK_REASONING_EFFORT": "high",
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
    return systemd_env(values)


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
    build_archive(archive, find_jar())

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
        wrapper = f"""#!/usr/bin/env bash
set -euo pipefail

root={APP_ROOT}
release={release}
archive={remote_archive}
previous="$(readlink -f "$root/current" 2>/dev/null || true)"
before_nginx_pid="$(systemctl show nginx -p MainPID --value || true)"
before_quant_pid="$(systemctl show ai-quant-api -p MainPID --value || true)"
before_cockpit_pid="$(systemctl show enterprise-ai-cockpit -p MainPID --value || true)"

if [[ ! -x /usr/local/bin/java17 ]]; then
  dnf -y --disablerepo='epel*' install java-17-openjdk-headless >/dev/null
  java17="$(find /usr/lib/jvm -type f -path '*java-17*/bin/java' | head -n 1)"
  test -x "$java17"
  ln -sfn "$java17" /usr/local/bin/java17
fi
id aiapps >/dev/null 2>&1 ||   useradd --system --home-dir /opt/ai-platform --shell /sbin/nologin aiapps

mkdir -p "$root/releases/$release" "$root/shared" "$root/www"
tar -xzf "$archive" -C "$root/releases/$release"
chown -R aiapps:aiapps "$root/releases/$release" "$root/shared"
chmod 755 "$root" "$root/releases" "$root/www" "$root/releases/$release"

ln -sfn "$root/releases/$release" "$root/current.next"
mv -Tf "$root/current.next" "$root/current"
ln -sfn "$root/releases/$release/dist" "$root/www/crossBorderTrend.next"
mv -Tf "$root/www/crossBorderTrend.next" "$root/www/crossBorderTrend"

install -o aiapps -g aiapps -m 600   /tmp/crossborder-app.env "$root/shared/app.env"
install -m 644 /tmp/crossborder-trend.service   /etc/systemd/system/crossborder-trend.service
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
  if [[ -n "$previous" && -d "$previous" ]]; then
    ln -sfn "$previous" "$root/current.next"
    mv -Tf "$root/current.next" "$root/current"
    ln -sfn "$previous/dist" "$root/www/crossBorderTrend.next"
    mv -Tf "$root/www/crossBorderTrend.next" "$root/www/crossBorderTrend"
    systemctl restart crossborder-trend.service || true
  fi
  journalctl -u crossborder-trend.service -n 80 --no-pager
  exit 1
fi

systemctl is-active --quiet crossborder-trend.service
if command -v nginx >/dev/null 2>&1; then nginx -t; fi
test "$before_nginx_pid" =   "$(systemctl show nginx -p MainPID --value || true)"
test "$before_quant_pid" =   "$(systemctl show ai-quant-api -p MainPID --value || true)"
test "$before_cockpit_pid" =   "$(systemctl show enterprise-ai-cockpit -p MainPID --value || true)"

find "$root/releases" -mindepth 1 -maxdepth 1 -type d -printf '%T@ %p\\n'   | sort -nr | tail -n +6 | cut -d' ' -f2- | xargs -r rm -rf
rm -f "$archive" /tmp/crossborder-app.env   /tmp/crossborder-trend.service /tmp/deploy-crossborder-trend.sh

printf 'CROSSBORDER_SERVICE=%s\\n'   "$(systemctl is-active crossborder-trend.service)"
"""
        remote.upload_bytes(
            wrapper.encode("utf-8"),
            "/tmp/deploy-crossborder-trend.sh",
            0o700,
        )
        remote.run(
            "/bin/bash /tmp/deploy-crossborder-trend.sh",
            root=True,
            timeout=900,
        )
    finally:
        remote.close()
        archive.unlink(missing_ok=True)

    print(f"PUBLIC_URL=https://{remote.config['host']}/crossBorderTrend/")
    print(f"CROSSBORDER_RELEASE={release}")


if __name__ == "__main__":
    main()
