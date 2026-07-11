# 跨境选品数据源接入手册

本手册对应当前项目的 Spring Boot + Vue 架构。第一期目标是获取 Amazon/TikTok 的公开趋势、价格与热度数据，结合国内采购价和汇率生成选品报告；店铺订单与库存 API 放到第二期。

## 1. 开始前准备

1. 复制 `.env.example` 为 `.env`。
2. 开发阶段保持 `AUTH_ENABLED=false`；要体验 JWT 时设置 `AUTH_ENABLED=true`、`JWT_SECRET` 为随机长字符串。JWT 没有 Refresh Token，默认有效期由 `JWT_EXPIRE_MINUTES` 控制。
3. 所有 API Token 只放 `.env` 或 `credentials.txt`，不要写入源码、数据库备注和截图。
4. 需要接 OAuth 平台时，准备 HTTPS 域名和回调地址，例如 `https://your-domain.com/api/oauth/tiktok-shop/callback`。
5. 在后台的“数据源配置”中登记启用的数据源、市场和品类；采集前确认市场币种、物流费、平台费和税费规则。

## 2. 推荐接入顺序

| 顺序 | 数据源 | 解决的问题 | 是否需要店铺 |
|---|---|---|---|
| 1 | Rainforest API | Amazon 当前搜索、价格、评分、排名 | 否 |
| 2 | Keepa | ASIN 价格和 BSR 历史 | 否 |
| 3 | Apify TikTok Actor | 关键词、标签、视频互动趋势 | 否 |
| 4 | Frankfurter / ECB | JPY、USD、EUR、GBP 等日汇率 | 否 |
| 5 | 1688 报价导入/开放平台 | 国内采购成本 | 否/视权限而定 |
| 6 | Amazon SP-API、TikTok Shop、Shopify 等 | 自有店铺订单、库存、销售 | 是 |

## 3. Rainforest API：Amazon 当前商品数据

1. 在 [Rainforest API 文档](https://docs.trajectdata.com/rainforestapi/product-data-api/overview) 注册并创建 API Key。
2. 在 `.env` 设置：

```env
SOURCE_MODE=external
RAINFOREST_API_KEY=replace-with-your-key
AMAZON_MODE=rainforest
```

3. 先用 Postman 测试搜索接口，站点日本使用 `amazon.co.jp`：

```text
GET https://api.rainforestapi.com/request?api_key=YOUR_KEY&type=search&amazon_domain=amazon.co.jp&search_term=収納用品
```

4. 验证至少能拿到标题、ASIN、链接、价格、评分、评论数、排名或搜索位置。
5. 系统接入时先保存原始响应，再映射为统一字段：`sourcePlatform`、`externalProductId`、`title`、`price`、`currency`、`rating`、`reviewCount`、`heatScore`、`capturedAt`。

## 4. Keepa：Amazon 历史价格和排名

1. 在 [Keepa API](https://keepa.com/#!api) 购买/获取 Token。
2. 配置：

```env
KEEPA_API_KEY=replace-with-your-key
```

3. 用 Rainforest 搜索结果中的 ASIN 查询 Keepa。
4. 优先保存：历史最低价、当前价、BSR、BSR 变化、Buy Box 价格和卖家数。
5. Keepa Token 有成本，先只对 Rainforest/TikTok 初筛后的候选商品调用。

## 5. Apify：TikTok 公开趋势

1. 注册 [Apify](https://console.apify.com/)，进入 **API & Integrations** 创建 Token。
2. 在 Apify Store 选一个 TikTok Actor，记录 Actor ID；不同 Actor 的输入字段不同，先在控制台成功运行一次。
3. 配置：

```env
APIFY_TOKEN=replace-with-your-token
APIFY_TIKTOK_ACTOR=owner/actor-name
TIKTOK_MODE=apify
```

4. 通过 API 运行 Actor 并获取 Dataset：

```text
POST https://api.apify.com/v2/acts/ACTOR_ID/run-sync-get-dataset-items?token=YOUR_TOKEN
Content-Type: application/json
```

5. 输入关键词、Hashtag、地区、结果数量；输出映射 `video_id`、标题、标签、播放量、点赞、评论、分享、发布时间和链接。
6. 热度建议使用播放增速、互动率、近 7 日内容量和关键词重复度计算。完整调用方式见 [Apify 官方教程](https://docs.apify.com/academy/api/run-actor-and-retrieve-data-via-api)。

> TikTok Research API 面向审核通过的非营利研究，不作为商业选品第一期方案；TikTok Shop Partner API 用于已授权店铺，二者 Token 不通用。

## 6. 汇率：后台手工覆盖 + 自动同步

第一期以人民币作为利润结算币种。每天同步 JPY/CNY、USD/CNY、EUR/CNY、GBP/CNY、SGD/CNY、THB/CNY、MYR/CNY；接口失败时使用后台最后一次确认值。

- 免费易用： [Frankfurter](https://frankfurter.dev/)，支持日汇率与历史汇率。
- 官方参考： [ECB 数据 API](https://data.ecb.europa.eu/help/api/data-examples)。

每一笔报告保存 `base_currency`、`quote_currency`、`rate`、`rate_date`、`provider`，确保历史利润可复算。

## 7. 官方店铺 API（第二期）

### Amazon SP-API

准备专业卖家账号、开发者 Profile、应用、LWA Client、Refresh Token 和 Marketplace ID。SP-API 主要取得自己或已授权卖家的订单、库存、销售和报告，流程见 [Amazon onboarding](https://developer-docs.amazon.com/sp-api/docs/onboarding-overview)。

### TikTok Shop Partner API

在 Partner Center 注册开发者、创建 App、申请 API 权限、配置 Redirect URL、用测试店铺验证授权，最后让卖家授权。步骤见 [TikTok Shop Authorization Guide](https://partner.tiktokshop.com/docv2/page/authorization-guide-202309)。

### Shopify / Lazada / eBay

- Shopify：创建应用，申请 `read_products`、`read_orders`、`read_inventory` 等 scope，安装到店铺后获得 Token；见 [Shopify Admin Token](https://shopify.dev/docs/apps/build/authentication-authorization/access-tokens/generate-app-access-tokens-admin)。
- Lazada：注册开发者、申请应用类型和 API 权限，获得 App Key/Secret 后引导卖家 OAuth；见 [Lazada Getting Started](https://open.lazada.com/apps/doc/doc?docId=108130&nodeId=10533)。
- eBay：注册 Developer Program，创建 Sandbox/Production keyset，使用 OAuth；见 [eBay API Guide](https://developer.ebay.com/develop/guides-v2/get-started-with-ebay-apis)。

## 8. 项目内的数据改造约定

所有数据源必须先落到统一模型，不能把平台 JSON 直接写入报告表：

```text
raw_source_payloads     原始响应，便于重放和排错
normalized_products     统一商品、站点、外部商品 ID
product_metrics         价格、评分、评论、排名、热视频指标快照
exchange_rates          汇率快照和人工覆盖
supplier_quotes         1688/人工供应商报价
collection_jobs         采集任务状态、错误和耗时
```

报告的幂等业务键为：`market + report_date + source_mode + source_config_version`。相同业务键默认直接复用已生成报告；前端传 `force=true` 才重新采集。

利润统一按：

```text
销售收入 - 采购价 - 头尾程物流 - 平台佣金 - 支付手续费 - 税费 - 广告费 = 预计利润
```

所有金额用 `BigDecimal` 保存和计算；原币金额、汇率和人民币金额同时保存。
