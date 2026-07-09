# 跨境电商日本热品日报 Demo

项目路径：`E:\codes\crossborder-trend-report`

## 功能概览

- 前台选品大屏：市场导航、商品卡片、筛选列表
- 独立后台：`/admin`
- 后台菜单：用户管理、角色管理、菜单管理、数据源配置、市场配置、品类配置、采集频率配置、日报记录、商品池
- 默认管理员：`admin / admin`
- 后端支持动态调度与报表配置

## 目录说明

- `backend`：Spring Boot 后端
- `frontend`：Vite + Vue 前端
- `scripts`：Windows / Ubuntu 启停脚本
- `logs`：运行日志与进程状态文件（已忽略提交）

## 本地环境变量

先复制一份示例配置：

```powershell
Copy-Item .env.example .env
```

然后按实际情况修改 `.env`。

常用变量：

```env
MYSQL_HOST=127.0.0.1
MYSQL_PORT=3306
MYSQL_DATABASE=crossborder_trend_demo
MYSQL_USER=root
MYSQL_PASSWORD=change_me
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

可选参数：

```powershell
.\scripts\start-dev-windows.ps1 -ShowWindow
.\scripts\start-dev-windows.ps1 -SkipInstall
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
- `GET /api/admin/profile`
- `GET /api/admin/menus`
- `GET /api/admin/users`
- `GET /api/admin/roles`
- `GET /api/admin/markets`
- `GET /api/admin/categories`
- `GET /api/admin/schedules`
- `GET /api/admin/settings`
- `PUT /api/admin/settings`

## 数据库初始化

- 初始化脚本：`backend/src/main/resources/schema.sql`
- Spring Boot 启动时会按 `spring.sql.init.mode=always` 自动执行

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
