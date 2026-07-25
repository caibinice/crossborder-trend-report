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
JWT 密钥和 DeepSeek token 不得进入 Git。

systemd 模板把堆限制为 128MB，并限制 metaspace、direct memory、code
cache、线程栈、连接池和 Tomcat 线程。服务只监听回环地址；JWT 必须在
生产环境开启。回滚时把 `current` 指回上一 release，重启服务并检查：

```bash
curl -fsS http://127.0.0.1:8090/api/admin/auth/status
```

公网只暴露 Nginx 的 80/443；不要在安全组放开 8090。
