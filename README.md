# 跨境电商日本热品日报 Demo

项目路径：`D:\codes\crossborder-trend-report`

## 功能

- 前台选品大屏：左侧市场导航 + 商品卡片 + 筛选列表
- 市场导航：日本市场可用，美国市场/东南亚市场为数据源占位
- 独立后台：右上角“后台管理”新标签打开 `/admin`
- RuoYi 风格后台：左侧菜单、顶部导航、标签栏、工作台、系统管理、选品配置、报表管理
- 后台菜单：用户管理、角色管理、菜单管理、数据源配置、市场配置、品类配置、采集频率配置、日报记录、商品池
- 管理员登录：admin / admin
- 动态定时：后台保存 Cron 后，后端动态调度任务按新频率触发

## 启动

```powershell
cd D:\codes\crossborder-trend-report
.\run-dev.ps1
```

访问：

- 前台选品大屏：http://127.0.0.1:5174/
- 后台管理：http://127.0.0.1:5174/admin
- 后端健康检查：http://localhost:8090/api/health

## 手动启动

```powershell
. D:\devtools\env-dev.ps1
. D:\devtools\start-mysql.ps1

cd D:\codes\crossborder-trend-report\backend
mvn spring-boot:run

# 新 PowerShell
. D:\devtools\env-dev.ps1
cd D:\codes\crossborder-trend-report\frontend
npm install
npm run dev
```

## 后端 API

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

## 数据库

- database: `crossborder_trend_demo`
- user: `cross_demo`
- password: `cross_demo_123456`

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

当前没有 API Key 时使用 demo 趋势 + 国内搜索链接回退。后续可配置：

- `APIFY_TOKEN`
- `RAINFOREST_API_KEY`
- `KEEPA_API_KEY`
- `SERPAPI_KEY`
- TikTok Shop 开放平台应用信息
- 1688 开放平台应用信息

## 安全说明

当前 admin/admin 仅用于本地开发 demo。生产环境应接入 Spring Security、加密密码、HTTPS、CSRF 防护和权限分级。
