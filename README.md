# 跨境电商热品日报

适用于跨境电商选品与趋势分析的 Spring Boot 3 + Vue 3 项目。真实公开数据进入 MySQL 后，由统一看板、后台和利润模型展示。

## 功能概览

- 前台选品驾驶舱：多来源商品聚合、综合热度/销量指数/销售额指数切换、实时搜索趋势、汇率、多币种利润、卡片/表格筛选
- 独立后台：`/admin`
- 后台菜单：用户管理、角色管理、菜单管理、数据源配置、市场配置、品类配置、采集频率配置、日报记录、商品池
- 后台支持登录、主动注销、会话过期回登录页及登录/注销审计；开发模式可直接进入，但主动注销后仍需重新登录。
- Google Trends、Frankfurter 和 WooCommerce 公共目录开箱即用；Yahoo Japan、Rakuten、Rainforest 配凭证即接入
- 前后台统一 Apple 风格设计系统，支持浅色/深色主题和移动端抽屉；弹窗始终限制在视口内滚动
- 默认管理员：`admin / admin`
- 后端支持默认 10 个品类 × 每类 10 件的动态配额、来源均衡选取、幂等日报、多币种换算、DeepSeek V4 Pro Thinking high 翻译/评估和采集运行审计

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

[deepseek.api]
api_key=your_key

[rakuten.api]
application_id=your_application_id
access_key=your_access_key
affiliate_id=your_affiliate_id

[yahoo.shopping]
client_id=your_client_id
```

DeepSeek 默认调用 `deepseek-v4-pro`，启用 Thinking 并使用 `reasoning_effort=high`；真实 Token 只放 `credentials.txt`。后台 **选品配置 → 参数配置** 可以修改品类数、每类商品数、销量/销售额筛选口径，以及 `名称|含 {keyword} 的 URL` 格式的 1688、淘宝、拼多多等采购站点。

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
- `GET /api/trend-signals?region=JP&limit=20`
- `GET /api/exchange-rates/latest?base=JPY&quote=CNY`
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
- `GET /api/admin/collection-runs`
- `POST /api/admin/data-sources/{sourceKey}/test`
- `POST /api/admin/data-sources/{sourceKey}/collect`

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
- `trend_signals`
- `exchange_rates`
- `data_collection_runs`

## 真实数据源接入

开箱即用：

- Google Trends RSS：JP / US / SG 搜索趋势
- Frankfurter：JPY / USD 等币种兑 CNY 的公共参考汇率
- WooCommerce Store API：公开商品、价格、图片、类目和热销排序

配置凭证后可用：

- `RAINFOREST_API_KEY`
- `RAKUTEN_APPLICATION_ID` + `RAKUTEN_ACCESS_KEY`
- `YAHOO_SHOPPING_CLIENT_ID`
- `DEEPSEEK_API_KEY`（可选智能标准化）

日报中的“销量指数”和“销售额指数”是依据各公开来源提供的评论数、榜单位置、价格等可验证信号归一化得到的 1–100 代理指标，不冒充平台未公开的真实成交量/成交额；“综合热度”再结合 AI 跨境潜力评分计算并统一为 1–100。

预留扩展位：

- `APIFY_TOKEN`
- `KEEPA_API_KEY`
- `SERPAPI_KEY`
- `TIKTOK_RESEARCH_TOKEN`
- `TIKTOK_SHOP_API_KEY`

完整的账号材料、逐步操作、接口验证和 MySQL 验证 SQL：[`docs/cross-border-data-source-integration-guide.md`](docs/cross-border-data-source-integration-guide.md)。统一 UI 规范：[`docs/design-system.md`](docs/design-system.md)。

## 说明

- 仓库内只保留示例配置，不提交真实账号、密码、Token
- 生产环境建议接入正式鉴权、HTTPS、权限分级与审计
- 前端依赖已锁定版本；缺少依赖时启动脚本使用 `npm ci` 安装。
