# 跨境电商日本热品日报 Demo

项目路径：`D:\codes\crossborder-trend-report`

## 功能

- 日本热品日报：卡片视图 + 筛选列表
- 后台管理：配置国内/国外检索数据源、商品检索品类、区域、检索频率、汇率、物流成本、每日报表商品数
- 管理员登录：admin / admin
- 动态定时：后台保存 Cron 后，后端动态调度任务按新频率触发
- 数据源状态：展示 TikTok/Apify、TikTok Research、TikTok Shop、Amazon/Rainforest、Keepa、SerpAPI、1688 等配置状态

## 启动

```powershell
cd D:\codes\crossborder-trend-report
.\run-dev.ps1
```

访问：

- 前端：http://127.0.0.1:5174
- 后端：http://localhost:8090/api/health
- 后台：打开前端后点击“后台管理”

## 默认账号

```text
admin / admin
```

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

## 数据库

- database: `crossborder_trend_demo`
- user: `cross_demo`
- password: `cross_demo_123456`

## API

- `GET /api/health`
- `GET /api/datasources`
- `GET /api/reports/latest`
- `GET /api/report?date=2026-07-07`
- `POST /api/collect/run`
- `POST /api/admin/login`
- `GET /api/admin/settings`
- `PUT /api/admin/settings`

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
