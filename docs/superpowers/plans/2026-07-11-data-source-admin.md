# 数据源接入与后台运营能力 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在开发模式下提供可关闭的 JWT 认证、幂等报告生成、多币种利润计算和汇率快照，并将外部数据源配置、后台管理和接入教程落地。

**Architecture:** 后端保留现有 JDBC 与 Vue 单页结构。新增小型、无第三方依赖的 HMAC JWT 服务；报告层通过“已有报告直接复用”保证幂等；汇率作为可人工维护、可公共 API 同步的独立服务；外部数据源统一转为 `TrendCandidate`，未配置密钥时继续回退 Demo 数据。

**Tech Stack:** Java 17、Spring Boot 3、JdbcTemplate、JUnit 5、Vue 3、Vite、原生 Fetch。

---

### Task 1: 写入数据源接入手册

**Files:**
- Create: `docs/cross-border-data-source-integration-guide.md`

- [ ] **Step 1: 编写可执行的环境变量清单**

文档列出 `SOURCE_MODE`、Apify、Rainforest、Keepa、汇率与 TikTok Shop/Amazon SP-API 的开发与生产变量，并明确 Token 不提交到仓库。

- [ ] **Step 2: 编写逐平台接入步骤**

包含 Apify TikTok、Rainforest、Keepa、Frankfurter、Amazon SP-API、TikTok Shop、Lazada、Shopify 的“注册、取 Key、测试、配置、验证”步骤和官方链接。

- [ ] **Step 3: 编写项目改造章节**

说明原始数据、标准化商品、任务、汇率、供应商报价表的职责；说明 `TrendDataSource` 适配器模式与 Demo 回退规则。

### Task 2: 可开关 JWT 认证

**Files:**
- Create: `backend/src/main/java/com/example/crossborder/config/SecurityProperties.java`
- Create: `backend/src/main/java/com/example/crossborder/service/JwtTokenService.java`
- Create: `backend/src/test/java/com/example/crossborder/service/JwtTokenServiceTest.java`
- Modify: `backend/src/main/java/com/example/crossborder/service/AdminAuthService.java`
- Modify: `backend/src/main/java/com/example/crossborder/controller/AdminController.java`
- Modify: `backend/src/main/resources/application.yml`
- Modify: `.env.example`
- Modify: `frontend/src/components/AdminPage.vue`

- [ ] **Step 1: 写失败测试：签发的 JWT 能验证并能在过期后失效**

```java
@Test
void issueAndVerify_returnsClaimsBeforeExpiry() {
    JwtTokenService service = new JwtTokenService("test-secret", 60);
    String token = service.issue("admin", "admin");
    assertEquals("admin", service.verify(token).orElseThrow().username());
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `mvn test -Dtest=JwtTokenServiceTest`

- [ ] **Step 3: 实现最小 JWT 服务和配置开关**

JWT 使用 `HS256`、`sub`、`role`、`iat`、`exp`；`AUTH_ENABLED=false` 时 `authorized` 直接返回 true，登录仍返回开发 Token；不引入 Refresh Token。

- [ ] **Step 4: 前端读取 `/api/admin/auth/status`**

认证关闭时，后台页面不显示登录门槛并直接加载数据；认证开启时保留当前登录表单和 Authorization Header。

- [ ] **Step 5: 运行后端测试和前端构建**

Run: `mvn test -Dtest=JwtTokenServiceTest` and `npm.cmd run build`

### Task 3: 报告幂等、利润和汇率快照

**Files:**
- Create: `backend/src/main/java/com/example/crossborder/model/ExchangeRate.java`
- Create: `backend/src/main/java/com/example/crossborder/service/ExchangeRateService.java`
- Create: `backend/src/test/java/com/example/crossborder/service/ProfitCalculatorTest.java`
- Modify: `backend/src/main/java/com/example/crossborder/repository/TrendRepository.java`
- Modify: `backend/src/main/java/com/example/crossborder/service/TrendReportService.java`
- Modify: `backend/src/main/java/com/example/crossborder/model/RunCollectRequest.java`
- Modify: `backend/src/main/java/com/example/crossborder/repository/AdminDataRepository.java`
- Modify: `backend/src/main/java/com/example/crossborder/controller/AdminController.java`
- Modify: `backend/src/main/resources/schema.sql`

- [ ] **Step 1: 写失败测试：重复收集同一天同模式的报告不重复生成**

```java
@Test
void collectSameBusinessKey_returnsExistingReportWithoutSecondInsert() {
    // use in-memory repository fixture and assert report/product counts stay unchanged
}
```

- [ ] **Step 2: 写失败测试：利润扣除平台费、支付费和税费**

```java
@Test
void calculateProfit_subtractsAllConfiguredCosts() {
    assertEquals(new BigDecimal("32.00"), calculator.profit(...));
}
```

- [ ] **Step 3: 实现幂等键与可强制重采集**

默认重复请求返回已有报告；请求 `force=true` 时替换对应报告和商品明细。

- [ ] **Step 4: 实现汇率管理和利润字段**

新增 `exchange_rates` 表、后台读取/保存接口、Frankfurter 同步入口；按市场配置计算平台费、支付费、税费、物流费、利润和毛利率。

- [ ] **Step 5: 运行测试**

Run: `mvn test -Dtest=ProfitCalculatorTest,TrendRepositoryTest`

### Task 4: 外部数据源适配与 Demo 回退

**Files:**
- Create: `backend/src/main/java/com/example/crossborder/service/TrendDataSource.java`
- Create: `backend/src/main/java/com/example/crossborder/service/ExternalTrendDataSource.java`
- Create: `backend/src/test/java/com/example/crossborder/service/ExternalTrendDataSourceTest.java`
- Modify: `backend/src/main/java/com/example/crossborder/service/ExternalDataSourceService.java`
- Modify: `backend/src/main/java/com/example/crossborder/service/TrendReportService.java`
- Modify: `backend/src/main/java/com/example/crossborder/config/SourceProperties.java`

- [ ] **Step 1: 写失败测试：未配置 API Key 时使用空外部结果且报告回退 Demo**

```java
@Test
void fetch_withoutConfiguredProviders_returnsEmptyCandidates() {
    assertTrue(source.fetch(LocalDate.now(), settings).isEmpty());
}
```

- [ ] **Step 2: 实现统一数据源接口**

`TrendDataSource#fetch(LocalDate, AdminSettings)` 输出统一 `TrendCandidate`；`ExternalTrendDataSource` 支持 Rainforest 搜索和 Apify TikTok Dataset 的宽容字段映射。

- [ ] **Step 3: 让报告按 SOURCE_MODE 选择数据源**

`demo` 固定 Demo；`external` 优先外部结果、无结果回退 Demo；`mixed` 合并去重并排序。

- [ ] **Step 4: 运行后端测试**

Run: `mvn test -Dtest=ExternalTrendDataSourceTest`

### Task 5: 后台菜单覆盖与前端开发体验

**Files:**
- Modify: `frontend/src/components/AdminPage.vue`
- Modify: `frontend/src/lib.js`
- Modify: `backend/src/main/java/com/example/crossborder/config/AppConfig.java`
- Modify: `backend/src/main/java/com/example/crossborder/controller/ReportController.java`
- Modify: `backend/src/main/java/com/example/crossborder/controller/AdminController.java`

- [ ] **Step 1: 写失败测试：CORS 包含 PUT**

创建 MVC 配置测试，验证后台保存设置所需的 `PUT` 预检请求不会被拒绝。

- [ ] **Step 2: 实现后台缺口**

覆盖用户、角色、菜单、字典、参数、日志、数据源、市场、品类、采集频率、汇率、日报和商品池；租户入口从前端菜单隐藏，不删除数据库兼容字段。

- [ ] **Step 3: 改善前端请求和数据源配置页**

添加统一错误展示、认证状态加载、汇率入口、强制重采集开关、数据源说明与环境变量提示；API 请求不再为 GET 强塞 JSON Content-Type。

- [ ] **Step 4: 运行前端构建**

Run: `npm.cmd run build`

### Task 6: 验证、文档复核和提交

**Files:**
- Modify: `README.md`
- Modify: `docs/cross-border-data-source-integration-guide.md`

- [ ] **Step 1: 更新 README 的开发认证和真实数据源说明**

- [ ] **Step 2: 执行所有可用测试和构建**

Run: `mvn test` and `npm.cmd run build`

- [ ] **Step 3: 检查 Git diff 与未跟踪文件**

Run: `git status --short` and `git diff --check`

- [ ] **Step 4: 提交功能分支**

Run: `git add ... && git commit -m "feat: add data source operations foundation"`
