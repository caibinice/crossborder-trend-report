# 跨境电商热品日报

适用于跨境电商选品与趋势分析的 Spring Boot 3 + Vue 3 项目。真实公开数据进入 MySQL 后，由统一看板、后台和利润模型展示。

生产环境使用 `/crossBorderTrend/` 子路径；公开驾驶舱免登录，采集操作
通过后端口令换取 30 分钟 JWT，管理后台继续使用自身 JWT。低内存
发布、回滚与 systemd 参数见
[`docs/production-deployment.md`](docs/production-deployment.md)；所有
真实密钥只放在忽略的 `credentials.txt` 或服务器 `shared/app.env`。
单独开发本项目时只需克隆本仓库，并把私有 `credentials.txt` 放在根目录；
不要求下载 `ai-blog`。本地文件既支持无前缀项目段，也支持直接复制含
`crossborder.*` 段的通用凭据文件。兄弟目录中的博客凭据仅作为可选兼容
回退。

## 功能概览

- 前台选品驾驶舱：多来源商品聚合、综合热度/销量指数/销售额指数切换、实时搜索趋势、汇率、多币种利润、卡片/表格筛选
- 独立后台：`/admin`
- 后台菜单：用户管理、角色管理、菜单管理、数据源配置、市场配置、品类配置、采集频率配置、日报记录、商品池
- 后台支持登录、主动注销、会话过期回登录页及登录/注销审计；开发模式可直接进入，但主动注销后仍需重新登录。
- 日本、美国、东南亚三市场均已接入 Google Trends、Frankfurter 和独立 WooCommerce 公共目录；Yahoo Japan（高评价趋势榜 + 商品搜索）、Rakuten、Rainforest 配凭证后增强日本市场
- 前后台统一 Apple 风格设计系统，支持浅色/深色主题和移动端抽屉；弹窗始终限制在视口内滚动
- 生产管理员账号为 `admin`，密码仅由忽略的部署环境
  `FIXED_ADMIN_PASSWORD` 注入，不在仓库中提供默认值。
- 后端支持三个市场各自的幂等日报，默认 10 个品类 × 每类 20 件（每市场最多 200 件）、来源均衡选取、多币种换算和采集运行审计；三个市场统一使用中文页面、配置、日报与采购词，商品卡片保留来源原始名称并以中文翻译作为主标题

## 目录说明

- `backend`：Spring Boot 后端
- `frontend`：Vite + Vue 前端
- `scripts`：Windows / Ubuntu 启停脚本
- `logs`：运行日志与进程状态文件（已忽略提交）

## 独立构建、部署与提交

```powershell
# 只验证本地完整构建，不连接服务器
pwsh -File scripts/deploy.ps1 -BuildOnly

# 仅发布跨境项目；不会重启 Nginx、量化或智能座舱
pwsh -File scripts/deploy.ps1

# 通过本项目 credentials.txt 的 token 和 20808 代理提交推送
pwsh -File scripts/github-push.ps1 `
  -Message 'fix: describe the change' `
  -Files @('path/to/changed-file')
```

首次远程发布会在忽略的 `.venv-deploy` 安装 Paramiko，并在本项目
`.deploy` 生成独立签名密钥。现有服务器需已配置 `/crossBorderTrend/`
Nginx 路由；项目级发布只更新自己的 release、静态目录和 systemd 服务。

## 启动配置

默认启动目标已经改成远端数据库。

敏感信息统一放在 `credentials.txt`，推荐使用 INI 格式：

```ini
[mysql.remote]
host=caibinice.com
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
api-key=your_key

[rakuten.api]
application_id=your_application_id
access_key=your_access_key
affiliate_id=your_affiliate_id
api_base_url=https://openapi.rakuten.co.jp

[yahoo.shopping]
client_id=your_client_id
```

共享文件使用 `[crossborder.mysql.remote]`、
`[crossborder.deepseek.api]`、`[crossborder.rakuten.api]` 等名称；本地文件
仍使用上面的无前缀名称。DeepSeek 默认调用 `deepseek-v4-flash`，启用
Thinking 并使用 `reasoning_effort=max`；真实 Token 只放
`credentials.txt`。后台 **选品配置 → 参数配置** 可以修改品类数、每类
商品数、销量/销售额筛选口径，以及 `名称|含 {keyword} 的 URL` 格式的
1688、淘宝、拼多多等采购站点。

无需凭证的公共商品目录按市场独立配置：

```env
WOOCOMMERCE_STORE_URLS=https://www.somethingfromjapan.com
WOOCOMMERCE_US_STORE_URLS=https://thompsonhanson.com,https://helloyumi.com
WOOCOMMERCE_SEA_STORE_URLS=https://watchexchange.sg,https://publico.sg
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
- 启用认证（`AUTH_ENABLED=true`）并首次初始化空库时，必须设置 8–72 位
  `INITIAL_ADMIN_PASSWORD`。生产环境还设置 `FIXED_ADMIN_PASSWORD`，
  服务启动后会校准 `admin` 的 BCrypt 密码，使公开采集验证和后台登录使用
  同一口令。
- 新建用户必须设置密码，已有明文密码会在首次成功登录后自动升级为 BCrypt 哈希。

前端生产构建关闭 source map，将 Vue 拆为 `vendor-vue`，只对自有业务
chunk 做保守混淆。混淆不包含密钥，也不能代替 JWT 鉴权。

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
- WooCommerce Store API：按 JP / US / SEA 分开的公开商品、价格、图片、类目和热销排序；接口无需 API Key

配置凭证后可用：

- `RAINFOREST_API_KEY`
- `RAKUTEN_APPLICATION_ID` + `RAKUTEN_ACCESS_KEY`
- `YAHOO_SHOPPING_CLIENT_ID`（Yahoo 高评价趋势榜优先，商品搜索 V3 兜底）
- `DEEPSEEK_API_KEY`（可选智能标准化）

日报中的“销量指数”和“销售额指数”是依据各公开来源提供的评论数、榜单位置、价格等可验证信号归一化得到的 1–100 代理指标，不冒充平台未公开的真实成交量/成交额；“综合热度”再结合 AI 跨境潜力评分计算并统一为 1–100。

预留扩展位：

- `APIFY_TOKEN`
- `KEEPA_API_KEY`
- `SERPAPI_KEY`
- `TIKTOK_RESEARCH_TOKEN`
- `TIKTOK_SHOP_API_KEY`

完整的账号材料、逐步操作、接口验证和 MySQL 验证 SQL：[`docs/cross-border-data-source-integration-guide.md`](docs/cross-border-data-source-integration-guide.md)。Yahoo 旧 SDK 审查结论和专项接入步骤：[`docs/yahoo-shopping-integration-assessment.md`](docs/yahoo-shopping-integration-assessment.md)。统一 UI 规范：[`docs/design-system.md`](docs/design-system.md)。

## 说明

- 仓库内只保留示例配置，不提交真实账号、密码、Token
- 生产环境建议接入正式鉴权、HTTPS、权限分级与审计
- 前端依赖已锁定版本；缺少依赖时启动脚本使用 `npm ci` 安装。
