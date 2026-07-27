# Yahoo! Japan Shopping 接入评估与操作手册

## 1. 结论

`shoprev/yahoo-api` 不应作为本项目依赖。它是 2013 年发布的 Ruby SDK，
内部硬编码 `http://shopping.yahooapis.jp/ShoppingWebService/V1/...`
旧接口，并且仍要求调用方提供 `appid`。其中旧版 Category Ranking V1、
Query Ranking V1 等接口已经停止服务，SDK 也不能提供免凭证商品或销售数据。

本项目直接使用 Yahoo 官方 HTTPS 接口：

1. **高评价趋势榜**：
   `ShoppingWebService/V1/highRatingTrendRanking`；
2. **商品搜索 V3 兜底**：
   `ShoppingWebService/V3/itemSearch`。

旧仓库仅作为实现审查样本下载到了
`E:\codes\shoprev-yahoo-api`，没有复制进本仓库，也没有引入 Ruby 运行时。

## 2. 能拿到什么数据

| 能力 | 高评价趋势榜 | 商品搜索 V3 |
|---|---|---|
| 商品名、商品链接、商品代码 | 是 | 是 |
| 含税价格、图片 | 是 | 是 |
| 评分、评论数 | 是 | 是 |
| 官方趋势名次 | 是 | 否 |
| 跨境代购、在库、全新筛选 | 否 | 是 |
| 真实销量、下单人数、销售额 | **不返回** | **不返回** |

Yahoo 官方说明高评价趋势榜会综合“下单人数”和用户评论等多个指标，但
响应只提供名次、商品信息和评论，不提供原始下单人数。因此系统把官方名次、
评论数、评分和价格转换为来源内 1–100 指数，并在商品理由中明确标注为代理
信号，不伪装成真实成交数据。

官方文档：

- <https://developer.yahoo.co.jp/webapi/shopping/shopping/v1/highRatingTrendRanking.html>
- <https://developer.yahoo.co.jp/webapi/shopping/v3/itemsearch.html>
- <https://developer.yahoo.co.jp/webapi/shopping/v2/queryranking.html>
- <https://developer.yahoo.co.jp/webapi/shopping/>

## 3. 你需要准备的材料

1. 可登录 Yahoo! JAPAN Developer Network 的账号；
2. 应用名称；
3. 服务说明和网站 URL；
4. 联系人信息；
5. 创建服务端应用后得到的 **Client ID（appid）**。

申请入口及说明：

- <https://developer.yahoo.co.jp/yconnect/v2/registration.html>
- <https://developer.yahoo.co.jp/webapi/shopping/help/application.html>

不需要 Yahoo 店铺账号、店铺 OAuth、订单权限或数据库改表。

## 4. 本地接入：逐步操作

### 第 1 步：申请 Client ID

在 Yahoo! JAPAN Developer Network 创建服务端应用，填写本项目的服务
URL 和用途。获得 Client ID 后不要放进 Java、Vue、`.env.example` 或 Git。

### 第 2 步：写入本地凭证

编辑项目根目录下被 Git 忽略的 `credentials.txt`：

```ini
[yahoo.shopping]
client_id=你的_Client_ID
```

Windows 和 Ubuntu 启动脚本都会把它加载成
`YAHOO_SHOPPING_CLIENT_ID`。

### 第 3 步：启动并测试

```powershell
cd E:\codes\crossborder-trend-report
.\scripts\start-dev-windows.ps1
```

进入“管理后台 → 数据源配置 → Yahoo! Japan Shopping”，确认状态为
“已配置”，然后点击“测试连接”。测试只查询第一个启用品类和 3 个商品，
不会一次跑完全部品类。

### 第 4 步：加入日报

在后台设置的境外数据源中保留 `Yahoo Shopping`，设置搜索品类、
最大品类数和每类商品数，然后点击前台“采集最新商品”。完整采集会：

1. 按品类转换为日文查询词；
2. 优先读取高评价趋势榜；
3. 趋势榜无结果时回退到商品搜索 V3；
4. 与 Rakuten、WooCommerce 等来源去重；
5. 可选调用 DeepSeek 翻译和评分；
6. 统一计算多币种成本、利润与 1–100 排名；
7. 写入云端 MySQL 的日报和商品表。

## 5. 生产部署

把 Client ID 写入服务器的共享环境文件，不能写进 release 目录：

```bash
/opt/crossborder-trend-report/shared/app.env
```

内容增加：

```env
YAHOO_SHOPPING_CLIENT_ID=你的_Client_ID
```

重启服务后检查：

```bash
systemctl restart crossborder-trend-report
curl -fsS http://127.0.0.1:8090/api/health
```

随后登录管理后台执行 Yahoo 数据源连接测试。当前正式环境只有配置该变量
后，健康状态和数据源中心才会把 Yahoo 识别为已配置来源。

## 6. 常见问题

### 返回 403

- Client ID 未配置、复制错误或应用状态不可用；
- 请求未从获准的应用环境发出；
- 旧 SDK 使用了已经淘汰的 HTTP/V1 接口。

先检查进程环境中是否存在 `YAHOO_SHOPPING_CLIENT_ID`，不要把完整值打印到
日志。用后台“测试连接”观察经过脱敏的错误信息。

### 为什么页面没有真实销量

公开 Shopping API 没有该字段。高评价趋势榜只说明排序综合了下单人数和评论，
不会返回订单数；要获得店铺真实销量或销售额，必须使用商家后台导出、店铺订单
API 或获得商家授权的数据，不能从这个 GitHub SDK 补出来。

### 是否还需要接 Query Ranking V2

它返回热门或上升的搜索词，适合增强“搜索趋势”而不是商品销售榜。当前项目已用
Google Trends 生成搜索趋势，因此先不增加一次重复调用；后续有 Yahoo Client ID
并完成线上额度验证后，可把 Query Ranking V2 作为日本市场关键词信号补充。

### 调用频率

适配器在同一个进程内把 Yahoo 请求间隔控制为至少 2.1 秒。某个品类请求失败时
不会清空已经取得的其他品类；遇到 401、403 或 429 会停止该轮 Yahoo 采集，避免
用无效凭证或被限流时继续请求。
