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
仓库内 `scripts/deploy.ps1` 是独立发布入口，默认只读取项目根目录
`credentials.txt`；文件可使用无前缀项目段，也可直接使用通用文件中的
`crossborder.*` 段。项目文件缺失时才兼容读取兄弟目录博客凭据，但博客
不是部署前置条件。脚本会将 Rakuten、Yahoo 等配置写入服务器
`shared/app.env`，并且只重启跨境服务，不改动其他项目或 Nginx。每次发布
都会先备份数据库、环境文件和 systemd 单元；健康检查失败时自动恢复旧
release、静态目录、环境与服务配置。发布结束还会校验 AI Blog release、
Nginx 配置及量化/智能座舱进程均未变化，并回归所有公开入口。已有部署会
保留服务器中的 JWT 与操作口令；首次部署则需在项目凭据中配置
`[platform.action] password`。
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
