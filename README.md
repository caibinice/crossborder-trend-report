# 跨境电商热品日报

适用于跨境电商的销售热品日报前后端项目，springboot+vue3

## 功能概览

- 前台选品大屏：市场导航、商品卡片、筛选列表
- 独立后台：`/admin`
- 后台菜单：用户管理、角色管理、菜单管理、数据源配置、市场配置、品类配置、采集频率配置、日报记录、商品池
- 后台支持登录、主动注销、会话过期回登录页及登录/注销审计；开发模式可直接进入，但主动注销后仍需重新登录。
- 默认管理员：`admin / admin`
- 后端支持动态调度与报表配置

## 目录说明

- `backend`：Spring Boot 后端
- `frontend`：Vite + Vue 前端
- `scripts`：Windows / Ubuntu 启停脚本
- `logs`：运行日志与进程状态文件（已忽略提交）

## 启动配置

默认启动目标已经改成远端数据库。

敏感信息统一放在 `credentials.txt`，推荐使用 INI 格式：

```ini
[mysql.remote]
host=101.132.78.217
port=3306
database=crossborder_trend_demo
user=cross_demo
password=your_password

[mysql.local]
host=127.0.0.1
port=3306
database=crossborder_trend_demo
user=root
password=
```

`.env` 只放非敏感开关，例如：

```powershell
Copy-Item .env.example .env
```

默认远端：

```env
DB_TARGET=remote
SERVER_PORT=8090
FRONTEND_HOST=127.0.0.1
FRONTEND_PORT=5174
DB_POOL_MAX_SIZE=3
DB_POOL_MIN_IDLE=0
```

如果要切本地，可新建 `.env.local`：

```env
DB_TARGET=local
SERVER_PORT=8090
FRONTEND_HOST=127.0.0.1
FRONTEND_PORT=5174
```

## 启动方式

### Windows

```powershell
cd E:\codes\crossborder-trend-report
.\scripts\start-dev-windows.ps1
```

启动命令默认后台启动并立即返回到当前 PowerShell。需要等待两个端口完成就绪检查时使用：

```powershell
.\scripts\start-dev-windows.ps1 -WaitForReady
```

可选参数：

```powershell
.\scripts\start-dev-windows.ps1 -ShowWindow
.\scripts\start-dev-windows.ps1 -SkipInstall
.\scripts\start-dev-windows.ps1 -EnvFile .\.env.local
.\run-dev.ps1
```

停止：

```powershell
.\scripts\stop-dev-windows.ps1
```

查看状态：

```powershell
.\scripts\status-dev-windows.ps1
```

### Ubuntu 22.04

```bash
cd /path/to/crossborder-trend-report
cp .env.example .env
chmod +x scripts/*.sh
./scripts/start-dev-ubuntu.sh
```

停止：

```bash
./scripts/stop-dev-ubuntu.sh
```

查看状态：

```bash
./scripts/status-dev-ubuntu.sh
```

## 访问地址

- 前台：`http://127.0.0.1:5174/`
- 后台：`http://127.0.0.1:5174/admin`
- 后端健康检查：`http://localhost:8090/api/health`

## 常用后端 API

- `GET /api/health`
- `GET /api/datasources`
- `GET /api/reports/latest`
- `GET /api/report?date=2026-07-07`
- `POST /api/collect/run`
- `POST /api/admin/login`
- `POST /api/admin/logout`
- `GET /api/admin/profile`
- `GET /api/admin/menus`
- `GET /api/admin/users`
- `GET /api/admin/roles`
- `GET /api/admin/markets`
- `GET /api/admin/categories`
- `GET /api/admin/schedules`
- `GET /api/admin/settings`
- `PUT /api/admin/settings`

## 数据库迁移与初始化

- 结构迁移：`backend/src/main/resources/db/migration/`
- Spring Boot 使用 Flyway；已有数据库首次启动会以版本 `0` 建立基线并执行安全迁移。
- 启动不再用演示 SQL 覆盖用户、角色、菜单、市场、品类、系统设置或管理员密码；首次**完全空库**只初始化一次，并记录初始化标记，之后不会因访问或重启补回已删除的数据。
- 启用认证（`AUTH_ENABLED=true`）并首次初始化空库时，必须在 `.env` / 部署环境中设置 8–72 位的 `INITIAL_ADMIN_PASSWORD`。开发模式未设置时会生成随机初始密码，因开发鉴权默认关闭不影响本地调试。
- 新建用户必须设置密码，已有明文密码会在首次成功登录后自动升级为 BCrypt 哈希。

主要表：

- `trend_reports`
- `trend_products`
- `domestic_links`
- `admin_settings`
- `admin_users`
- `admin_roles`
- `admin_menus`
- `market_configs`
- `category_configs`

## 真实数据源接入

当前可通过环境变量接入外部数据源：

- `APIFY_TOKEN`
- `RAINFOREST_API_KEY`
- `KEEPA_API_KEY`
- `SERPAPI_KEY`
- `TIKTOK_RESEARCH_TOKEN`
- `TIKTOK_SHOP_API_KEY`

## 说明

- 仓库内只保留示例配置，不提交真实账号、密码、Token
- 生产环境建议接入正式鉴权、HTTPS、权限分级与审计
- 前端依赖已锁定版本；缺少依赖时启动脚本使用 `npm ci` 安装。
