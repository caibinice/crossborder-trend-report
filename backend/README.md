# Crossborder Trend Report Backend

默认端口 `8090`。

```powershell
. D:\devtools\env-dev.ps1
. D:\devtools\start-mysql.ps1
cd D:\codes\crossborder-trend-report\backend
mvn spring-boot:run
```

接口：`GET /api/health`、`GET /api/reports/latest`、`POST /api/collect/run`。

当前 `SOURCE_MODE=demo`，用内置样例和搜索链接，保证不依赖不稳定网页抓取也能跑通。后续可接 TikTok Shop、Apify、SerpAPI、1688/淘宝开放平台或你自己的爬虫服务。
