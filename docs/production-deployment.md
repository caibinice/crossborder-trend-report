# 生产部署

前端固定部署在 `/crossBorderTrend/`，API 通过同源
`/crossBorderTrend/api/` 反向代理到回环地址 `127.0.0.1:8090`。刷新
任意前端路由时，Nginx 应回退到该项目的 `index.html`。

生产服务器只需要 OpenJDK 17 headless；Maven 和 Node 构建都在本地
完成。每次发布上传 `backend/target/*.jar` 与 `frontend/dist` 到：

```text
/opt/crossborder-trend-report/releases/<commit>/
```

验证后原子更新 `current` 软链接，并只保留最近五版。环境变量从
`/opt/crossborder-trend-report/shared/app.env` 读取；可从
`deploy/application-production.env.example` 复制，但真实数据库密码、
JWT 密钥、统一操作口令、DeepSeek token、Rakuten 凭据和 Yahoo Client ID 不得进入 Git。
统一发布器优先读取项目本地 `credentials.txt`；本地文件不存在时读取
`ai-blog/credentials.txt` 的 `crossborder.*` 命名空间，并将 Rakuten
Application ID、Access Key 与 Affiliate ID 注入服务器 `shared/app.env`。
新发布机的四仓库固定目录、分支和一次性 bootstrap 步骤见兄弟仓库
`ai-blog/docs/new-machine-setup.md`；无需复制旧机器的 `.deploy` 或构建产物。
启用 Yahoo 数据源时，还必须通过发布器或服务器环境文件写入
`YAHOO_SHOPPING_CLIENT_ID`；仓库只保留空值/占位值。
阿里云中国大陆出口默认使用 Rakuten 官方 CNAME 网关，绕过被污染的域名
解析；后端仍校验证书链以及官方 `openapi.rakuten.co.jp` 主机名。

systemd 模板把堆限制为 128MB，并限制 metaspace、direct memory、code
cache、线程栈、连接池和 Tomcat 线程。服务只监听回环地址；JWT 必须在
生产环境开启。`FIXED_ADMIN_PASSWORD` 只保存在 `shared/app.env`，公开
前台的手动采集先调用 `/api/action-auth/verify`，后台自动调度直接调用
服务层，不需要网页口令。回滚时把 `current` 指回上一 release，重启服务并检查：

```bash
curl -fsS http://127.0.0.1:8090/api/admin/auth/status
```

公网只暴露 Nginx 的 80/443；不要在安全组放开 8090。
