<template>
  <section v-if="showLogin" class="admin-login-page">
    <ThemeToggle class="login-theme" />
    <div class="login-visual">
      <div class="brand-lockup inverse"><span class="brand-mark"><AppIcon name="sparkles" /></span><div><strong>Northstar</strong><small>跨境趋势情报</small></div></div>
      <div class="login-visual-copy"><span class="overline">COMMERCE INTELLIGENCE OS</span><h1>让每一次选品<br />都有<em>真实数据</em>支撑</h1><p>统一管理商品目录、搜索趋势、汇率、利润模型与团队权限。</p></div>
      <div class="login-proof"><span><AppIcon name="database" />云端 MySQL</span><span><AppIcon name="shield" />JWT 会话</span><span><AppIcon name="activity" />实时采集</span></div>
    </div>
    <form class="admin-login-card" @submit.prevent="doLogin">
      <div class="login-card-head"><span class="metric-icon blue"><AppIcon name="shield" /></span><div><h2>欢迎回来</h2><p>登录 Northstar 管理控制台</p></div></div>
      <p class="login-mode"><span class="pulse-dot" />{{ authEnabled ? 'JWT 鉴权已开启' : '开发模式 · 主动注销后仍需登录' }}</p>
      <label class="field"><span>账号</span><input v-model.trim="login.username" autocomplete="username" placeholder="请输入账号" /></label>
      <label class="field"><span>密码</span><input v-model="login.password" type="password" autocomplete="current-password" placeholder="请输入密码" /></label>
      <button class="primary-button login-submit" :disabled="loggingIn"><AppIcon :name="loggingIn ? 'refresh' : 'arrow'" :class="{ spinning: loggingIn }" />{{ loggingIn ? '正在验证…' : '登录控制台' }}</button>
      <RouterLink class="login-back" to="/">返回选品驾驶舱</RouterLink>
      <p v-if="notice.text" :class="['inline-alert', notice.type]"><AppIcon :name="notice.type === 'error' ? 'warning' : 'check'" />{{ notice.text }}</p>
    </form>
  </section>

  <main v-else class="app-layout admin-layout">
    <aside :class="['app-sidebar', { open: sidebarOpen }]">
      <div class="brand-lockup"><span class="brand-mark"><AppIcon name="sparkles" /></span><div><strong>Northstar</strong><small>管理控制台</small></div></div>
      <div class="sidebar-label">工作空间</div>
      <nav class="sidebar-nav admin-nav">
        <template v-for="menu in navigationMenus" :key="menu.menuKey">
          <button :class="['nav-item', { active: activeMenu === menu.menuKey || menu.children?.some((child) => child.menuKey === activeMenu) }]" @click="selectMenu(menu)">
            <span class="nav-icon"><AppIcon :name="iconName(menu.icon)" /></span><span><b>{{ menu.title }}</b></span>
            <AppIcon v-if="menu.component === 'Layout'" name="chevron" :class="['nav-chevron', { expanded: expandedMenus.has(menu.menuKey) }]" />
          </button>
          <div v-if="menu.component === 'Layout' && expandedMenus.has(menu.menuKey)" class="nav-children">
            <button v-for="child in menu.children || []" :key="child.menuKey" :class="['nav-item child-nav', { active: activeMenu === child.menuKey }]" @click="selectMenu(child)"><span class="nav-icon"><AppIcon :name="iconName(child.icon)" /></span><span><b>{{ child.title }}</b></span></button>
          </div>
        </template>
      </nav>
      <div class="sidebar-spacer" />
      <div class="sidebar-account"><span class="avatar">{{ (profile?.nickname || 'A').slice(0, 1).toUpperCase() }}</span><div><b>{{ profile?.nickname || '管理员' }}</b><small>{{ profile?.roleName || profile?.username || 'admin' }}</small></div><button class="icon-button" title="注销登录" :disabled="loggingOut" @click="logout"><AppIcon name="logout" /></button></div>
    </aside>
    <button v-if="sidebarOpen" class="sidebar-scrim" aria-label="关闭导航" @click="sidebarOpen = false" />

    <section class="app-workspace admin-workspace">
      <header class="app-topbar">
        <div class="topbar-title"><button class="icon-button mobile-menu" aria-label="打开导航" @click="sidebarOpen = true"><AppIcon name="menu" /></button><div><small>管理控制台 / {{ activeTitle }}</small><strong>{{ activeTitle }}</strong></div></div>
        <div class="topbar-actions"><RouterLink class="button secondary-button desktop-action" to="/"><AppIcon name="dashboard" />选品看板</RouterLink><ThemeToggle /><button class="button secondary-button logout-button" :disabled="loggingOut" @click="logout"><AppIcon name="logout" />{{ loggingOut ? '注销中…' : '注销' }}</button></div>
      </header>

      <div class="page-container admin-page-container">
        <section v-if="activeMenu === 'dashboard'" class="dashboard-page">
          <article class="admin-welcome-panel"><div><span class="overline"><AppIcon name="sparkles" />GOOD DAY, {{ profile?.username || 'ADMIN' }}</span><h1>跨境业务工作台</h1><p>真实趋势、商品目录与运营配置都在一个控制面板中。</p></div><RouterLink class="button primary-button" to="/"><AppIcon name="arrow" />查看选品驾驶舱</RouterLink></article>
          <section class="metric-grid admin-metrics">
            <article><span class="metric-icon blue"><AppIcon name="users" /></span><div><small>团队用户</small><b>{{ users.length }}</b><p>{{ roles.length }} 个角色</p></div></article>
            <article><span class="metric-icon violet"><AppIcon name="database" /></span><div><small>数据能力</small><b>{{ configuredSourceCount }}</b><p>{{ sources.length }} 个适配位</p></div></article>
            <article><span class="metric-icon green"><AppIcon name="chart" /></span><div><small>日报记录</small><b>{{ reportSummaries.length }}</b><p>云端历史快照</p></div></article>
            <article><span class="metric-icon amber"><AppIcon name="activity" /></span><div><small>实时趋势</small><b>{{ trendSignals.length }}</b><p>当前缓存信号</p></div></article>
          </section>
          <section class="admin-dashboard-grid">
            <article class="surface-panel dashboard-source-panel"><div class="panel-heading"><div><span class="overline">PIPELINE HEALTH</span><h2>数据链路状态</h2></div><RouterLink to="/admin/selection/sources">查看全部</RouterLink></div><div class="pipeline-list"><div v-for="source in sources.slice(0, 5)" :key="source.key" class="pipeline-item"><span :class="['metric-icon', source.configured ? 'green' : 'neutral']"><AppIcon :name="sourceIcon(source.type)" /></span><div><b>{{ source.name }}</b><small>{{ source.note }}</small></div><span :class="['status-pill', source.configured ? 'success' : 'pending']">{{ source.configured ? '就绪' : '待配置' }}</span></div></div></article>
            <article class="surface-panel dashboard-run-panel"><div class="panel-heading"><div><span class="overline">RECENT RUNS</span><h2>最近采集</h2></div></div><div v-if="collectionRuns.length" class="run-timeline"><div v-for="run in collectionRuns.slice(0, 6)" :key="run.id"><span :class="['timeline-dot', run.status]" /><div><b>{{ sourceName(run.sourceKey) }}</b><small>{{ run.message }}</small></div><time>{{ formatDateTime(run.startedAt) }}</time></div></div><div v-else class="compact-empty"><AppIcon name="clock" /><div><b>暂无采集记录</b><p>在数据源配置中执行首次同步。</p></div></div></article>
          </section>
        </section>

        <AdminCrudPage v-if="activeMenu === 'users'" title="用户管理" description="管理登录账号、角色和状态；编辑时密码留空会保持原密码。" :rows="users" :columns="userColumns" :fields="userFields" :empty="emptyUser" @save="saveUser" @delete="deleteUser" />

        <section v-if="activeMenu === 'roles'" class="admin-panel">
          <div class="crud-head"><div><span class="overline">ACCESS CONTROL</span><h2>角色管理</h2><p class="muted">角色菜单与左侧导航实时联动。</p></div><button class="primary-button" @click="editingRole = { ...emptyRole }">新增角色</button></div>
          <AdminDataTable :rows="roles" :columns="roleColumns" actions @edit="editingRole = { ...$event, menuKeys: [...($event.menuKeys || [])] }" @delete="deleteRole" />
          <AdminModalForm v-if="editingRole" title="角色表单" :model="editingRole" :fields="roleFields" @save="saveRole" @cancel="editingRole = null"><template #default="{ draft }"><div class="permission-field"><span>菜单权限</span><div class="check-grid"><label v-for="menu in flatMenus" :key="menu.menuKey"><input v-model="draft.menuKeys" type="checkbox" :value="menu.menuKey" /><span>{{ menu.title }}</span></label></div></div></template></AdminModalForm>
        </section>

        <AdminCrudPage v-if="activeMenu === 'menus'" title="菜单管理" description="调整菜单后，后台导航和角色权限会同步更新。" :rows="flatMenus" :columns="menuColumns" :fields="menuFields" :empty="emptyMenu" @save="saveMenu" @delete="deleteMenu" />
        <AdminCrudPage v-if="activeMenu === 'dict'" title="字典类型" :rows="dictTypes" :columns="dictTypeColumns" :fields="dictTypeFields" :empty="emptyDictType" @save="saveDictType" @delete="deleteDictType"><template #extra><div class="subsection-heading"><div><h3>字典数据</h3><p>维护当前字典类型下的可选值。</p></div><button class="secondary-button" @click="editingDictData = { ...emptyDictData }">新增字典数据</button></div><AdminDataTable :rows="dictData" :columns="dictDataColumns" actions @edit="editingDictData = { ...$event }" @delete="deleteDictData" /><AdminModalForm v-if="editingDictData" title="字典数据" :model="editingDictData" :fields="dictDataFields" @save="saveDictData" @cancel="editingDictData = null" /></template></AdminCrudPage>
        <AdminCrudPage v-if="activeMenu === 'configs'" title="参数配置" description="集中维护系统级业务参数。" :rows="configs" :columns="configColumns" :fields="configFields" :empty="emptyConfig" @save="saveConfig" @delete="deleteConfig" />
        <section v-if="activeMenu === 'logs'" class="admin-panel"><div class="crud-head"><div><span class="overline">AUDIT TRAIL</span><h2>操作日志</h2></div></div><AdminDataTable :rows="operLogs" :columns="operLogColumns" /><div class="subsection-heading"><div><h3>登录日志</h3><p>登录成功、失败和主动注销记录。</p></div></div><AdminDataTable :rows="loginLogs" :columns="loginLogColumns" /></section>

        <section v-if="activeMenu === 'sources'" class="source-management-page">
          <article class="admin-welcome-panel source-hero"><div><span class="overline"><AppIcon name="database" />DATA SOURCE HUB</span><h1>真实数据源中心</h1><p>先测试连接，再同步趋势/汇率或采集商品日报；所有结果直接写入云端 MySQL。</p></div><button class="primary-button" :disabled="catalogCollecting" @click="collectCatalog"><AppIcon :name="catalogCollecting ? 'refresh' : 'sparkles'" :class="{ spinning: catalogCollecting }" />{{ catalogCollecting ? '正在采集…' : '采集商品并生成日报' }}</button></article>
          <article class="surface-panel source-settings-panel"><div class="panel-heading"><div><span class="overline">COLLECTION POLICY</span><h2>采集策略</h2></div></div><AdminSettingsForm v-if="settings" :settings="settings" section="sources" @save="saveSettings" /></article>
          <div class="source-card-grid">
            <article v-for="source in sources" :key="source.key" class="source-card">
              <div class="source-card-head"><span :class="['metric-icon', source.configured ? 'green' : 'neutral']"><AppIcon :name="sourceIcon(source.type)" /></span><div><span class="source-type">{{ source.type }}</span><h3>{{ source.name }}</h3></div><span :class="['status-pill', source.configured ? 'success' : 'pending']">{{ source.configured ? (source.live ? '免 Key 已连接' : '已配置') : '待配置' }}</span></div>
              <p>{{ source.useCase }}</p>
              <div class="source-meta"><div><span>接入模式</span><b>{{ source.mode }}</b></div><div><span>所需材料</span><b>{{ (source.requiredMaterials || []).join('、') }}</b></div></div>
              <div v-if="source.environmentVariables?.length" class="env-list"><code v-for="item in source.environmentVariables" :key="item">{{ item }}</code></div>
              <p v-if="sourceResults[source.key]" :class="['source-result', sourceResults[source.key].success ? 'success' : 'error']"><AppIcon :name="sourceResults[source.key].success ? 'check' : 'warning'" />{{ sourceResults[source.key].message }}</p>
              <div class="source-card-actions"><button v-if="source.supportsTest" class="secondary-button" :disabled="sourceBusy === source.key" @click="testSource(source)"><AppIcon :name="sourceBusy === source.key ? 'refresh' : 'activity'" :class="{ spinning: sourceBusy === source.key }" />测试连接</button><button v-if="source.supportsCollect" class="primary-button" :disabled="sourceBusy === source.key" @click="collectSource(source)"><AppIcon name="refresh" />立即同步</button><a :href="source.docsUrl" target="_blank" rel="noreferrer">官方文档<AppIcon name="external" /></a></div>
            </article>
          </div>
          <article class="surface-panel collection-log-panel"><div class="panel-heading"><div><span class="overline">INGESTION HISTORY</span><h2>采集记录</h2></div><button class="icon-button" aria-label="刷新采集记录" @click="reloadSourceData"><AppIcon name="refresh" /></button></div><AdminDataTable :rows="collectionRuns" :columns="collectionRunColumns" :searchable="false" /></article>
        </section>

        <AdminCrudPage v-if="activeMenu === 'markets'" title="市场配置" :rows="markets" :columns="marketColumns" :fields="marketFields" :empty="emptyMarket" @save="saveMarket" @delete="deleteMarket" />
        <AdminCrudPage v-if="activeMenu === 'categories'" title="品类配置" :rows="categories" :columns="categoryColumns" :fields="categoryFields" :empty="emptyCategory" @save="saveCategory" @delete="deleteCategory" />
        <section v-if="activeMenu === 'schedules'" class="admin-panel"><div class="crud-head"><div><span class="overline">AUTOMATION</span><h2>采集频率与利润基础</h2><p class="muted">定时任务会先同步趋势和汇率，再生成商品日报。</p></div></div><AdminSettingsForm v-if="settings" :settings="settings" section="schedule" @save="saveSettings" /></section>
        <section v-if="activeMenu === 'dailyReports'" class="admin-panel"><div class="crud-head"><div><span class="overline">REPORT ARCHIVE</span><h2>日报记录</h2></div></div><AdminDataTable :rows="reportSummaries" :columns="reportColumns" /></section>
        <section v-if="activeMenu === 'productPool'" class="admin-panel"><div class="crud-head"><div><span class="overline">PRODUCT CATALOG</span><h2>商品池</h2></div></div><p v-if="!productPool.length" class="muted">暂无日报商品，先在数据源中心采集商品。</p><AdminDataTable v-else :rows="productPool" :columns="productColumns" /></section>
      </div>
      <p v-if="notice.text && !showLogin" :class="['toast', notice.type]"><AppIcon :name="notice.type === 'error' ? 'warning' : 'check'" />{{ notice.text }}</p>
    </section>
  </main>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { api, currencyMoney, formatDateTime } from '../lib.js';
import AdminCrudPage from './AdminCrudPage.vue';
import AdminDataTable from './AdminDataTable.vue';
import AdminModalForm from './AdminModalForm.vue';
import AdminSettingsForm from './AdminSettingsForm.vue';
import AppIcon from './AppIcon.vue';
import ThemeToggle from './ThemeToggle.vue';

const route = useRoute();
const router = useRouter();
const token = ref(localStorage.getItem('adminToken') || '');
const signedOut = ref(localStorage.getItem('adminSignedOut') === 'true');
const login = reactive({ username: 'admin', password: '' });
const authEnabled = ref(null);
const loggingIn = ref(false);
const loggingOut = ref(false);
const loaded = ref(false);
const sidebarOpen = ref(false);
const notice = reactive({ type: '', text: '' });
const profile = ref(null);
const menus = ref([]);
const users = ref([]);
const roles = ref([]);
const dictTypes = ref([]);
const dictData = ref([]);
const configs = ref([]);
const operLogs = ref([]);
const loginLogs = ref([]);
const markets = ref([]);
const categories = ref([]);
const settings = ref(null);
const sources = ref([]);
const reportSummaries = ref([]);
const productPool = ref([]);
const collectionRuns = ref([]);
const trendSignals = ref([]);
const exchangeRate = ref(null);
const sourceBusy = ref('');
const catalogCollecting = ref(false);
const sourceResults = reactive({});
const editingRole = ref(null);
const editingDictData = ref(null);
const expandedMenus = ref(new Set(['system', 'selection', 'reports']));
const routeMenuKeys = { '/admin': 'dashboard', '/admin/system/users': 'users', '/admin/system/roles': 'roles', '/admin/system/menus': 'menus', '/admin/system/dict': 'dict', '/admin/system/configs': 'configs', '/admin/system/logs': 'logs', '/admin/selection/sources': 'sources', '/admin/selection/markets': 'markets', '/admin/selection/categories': 'categories', '/admin/selection/schedules': 'schedules', '/admin/reports/daily': 'dailyReports', '/admin/reports/products': 'productPool' };
const fallbackTitles = { dashboard: '工作台', users: '用户管理', roles: '角色管理', menus: '菜单管理', dict: '字典管理', configs: '参数配置', logs: '日志管理', sources: '数据源配置', markets: '市场配置', categories: '品类配置', schedules: '采集频率配置', dailyReports: '日报记录', productPool: '商品池' };

const navigationMenus = computed(() => removeTenant(menus.value));
const flatMenus = computed(() => flatten(navigationMenus.value));
const activeMenu = computed(() => flatMenus.value.find((menu) => menu.path === route.path)?.menuKey || routeMenuKeys[route.path] || 'dashboard');
const activeTitle = computed(() => flatMenus.value.find((menu) => menu.menuKey === activeMenu.value)?.title || fallbackTitles[activeMenu.value] || '工作台');
const showLogin = computed(() => signedOut.value || (authEnabled.value === true && !token.value));
const configuredSourceCount = computed(() => sources.value.filter((source) => source.configured).length);

function flatten(items) { return (items || []).flatMap((menu) => [menu, ...(menu.children ? flatten(menu.children) : [])]); }
function removeTenant(items) { return (items || []).filter((menu) => menu.menuKey !== 'tenants').map((menu) => ({ ...menu, children: removeTenant(menu.children) })); }
function tenantParam() { return encodeURIComponent(profile.value?.tenantId || 'default'); }
function currentTenantRow(row) { return { ...row, tenantId: profile.value?.tenantId || 'default' }; }
function setNotice(type, text) { notice.type = type; notice.text = text || ''; }
function errorMessage(error) { return error?.message || '请求失败，请稍后重试'; }
async function optional(path, options) { try { return await adminApi(path, options); } catch { return null; } }
async function adminApi(path, options = {}) { const headers = { ...(options.headers || {}) }; if (token.value) headers.Authorization = `Bearer ${token.value}`; return api(path, { ...options, headers }); }

function selectMenu(menu) {
  if (menu.component === 'Layout') { const next = new Set(expandedMenus.value); if (next.has(menu.menuKey)) next.delete(menu.menuKey); else next.add(menu.menuKey); expandedMenus.value = next; return; }
  sidebarOpen.value = false; router.push(menu.path || '/admin');
}

async function doLogin() {
  loggingIn.value = true;
  try {
    const response = await api('/admin/login', { method: 'POST', body: JSON.stringify(login) });
    token.value = response.token; localStorage.setItem('adminToken', response.token); localStorage.removeItem('adminSignedOut'); signedOut.value = false; login.password = '';
    const success = await loadAll(); if (success) setNotice('success', `欢迎回来，${response.username}`); else clearSession();
  } catch (error) { login.password = ''; setNotice('error', errorMessage(error)); } finally { loggingIn.value = false; }
}

function resetSessionData() {
  loaded.value = false; profile.value = null; menus.value = []; users.value = []; roles.value = []; dictTypes.value = []; dictData.value = []; configs.value = []; operLogs.value = []; loginLogs.value = []; markets.value = []; categories.value = []; settings.value = null; sources.value = []; reportSummaries.value = []; productPool.value = []; collectionRuns.value = []; trendSignals.value = []; exchangeRate.value = null;
}
function clearSession(message, type = 'success') { localStorage.removeItem('adminToken'); localStorage.setItem('adminSignedOut', 'true'); token.value = ''; signedOut.value = true; resetSessionData(); if (message) setNotice(type, message); }
async function logout() { loggingOut.value = true; try { await adminApi('/admin/logout', { method: 'POST' }); } catch (error) { if (error?.status !== 401) setNotice('error', errorMessage(error)); } finally { clearSession('已注销，请重新登录'); await router.replace('/admin'); loggingOut.value = false; } }

async function loadAll() {
  try {
    const status = await api('/admin/auth/status'); authEnabled.value = status.enabled; if (signedOut.value || (status.enabled && !token.value)) return true;
    const [profileData, menuData, settingData, sourceData, summaryData] = await Promise.all([adminApi('/admin/profile'), adminApi('/admin/menus'), adminApi('/admin/settings'), api('/datasources'), api('/reports/summaries?limit=30')]);
    profile.value = profileData; menus.value = menuData; settings.value = settingData; sources.value = sourceData; reportSummaries.value = summaryData;
    await Promise.all([reloadCoreLists(), reloadSourceData()]); loaded.value = true; await loadActiveData(activeMenu.value); return true;
  } catch (error) { if (error?.status === 401) clearSession('登录已过期，请重新登录', 'error'); else setNotice('error', errorMessage(error)); return false; }
}
async function reloadCoreLists() {
  const tenant = tenantParam();
  const [userData, roleData, dictTypeData, configData, marketData, categoryData] = await Promise.all([adminApi(`/admin/users?tenantId=${tenant}`), adminApi(`/admin/roles?tenantId=${tenant}`), adminApi(`/admin/dict-types?tenantId=${tenant}`), adminApi(`/admin/configs?tenantId=${tenant}`), adminApi(`/admin/markets?tenantId=${tenant}`), adminApi(`/admin/categories?tenantId=${tenant}`)]);
  users.value = userData; roles.value = roleData; dictTypes.value = dictTypeData; configs.value = configData; markets.value = marketData; categories.value = categoryData;
}
async function reloadSourceData() {
  const [runs, signals, rate, sourceData] = await Promise.all([optional('/admin/collection-runs?limit=30'), optional('/trend-signals?region=JP&limit=30'), optional('/exchange-rates/latest?base=JPY&quote=CNY'), api('/datasources')]);
  collectionRuns.value = runs || []; trendSignals.value = signals || []; exchangeRate.value = rate; sources.value = sourceData;
}
async function loadActiveData(menuKey) {
  if (!loaded.value) return; const tenant = tenantParam();
  try {
    if (menuKey === 'dict') dictData.value = await adminApi(`/admin/dict-data?tenantId=${tenant}`);
    if (menuKey === 'logs') [operLogs.value, loginLogs.value] = await Promise.all([adminApi(`/admin/oper-logs?tenantId=${tenant}`), adminApi(`/admin/login-logs?tenantId=${tenant}`)]);
    if (menuKey === 'sources' || menuKey === 'dashboard') await reloadSourceData();
    if (menuKey === 'productPool') { const latest = reportSummaries.value[0]; productPool.value = latest ? (await api(`/reports/${latest.id}`)).products.map((product) => ({ ...product, rank: `#${product.rank}`, sourcePriceText: currencyMoney(product.sourcePrice, product.sourceCurrency) })) : []; }
  } catch (error) { setNotice('error', errorMessage(error)); }
}

async function execute(work, successText, after) { try { await work(); if (after) await after(); setNotice('success', successText); return true; } catch (error) { setNotice('error', errorMessage(error)); return false; } }
async function testSource(source) { sourceBusy.value = source.key; try { sourceResults[source.key] = await adminApi(`/admin/data-sources/${source.key}/test`, { method: 'POST', body: JSON.stringify({ region: 'JP' }) }); } catch (error) { sourceResults[source.key] = { success: false, message: errorMessage(error) }; } finally { sourceBusy.value = ''; } }
async function collectSource(source) { sourceBusy.value = source.key; try { const run = await adminApi(`/admin/data-sources/${source.key}/collect`, { method: 'POST', body: JSON.stringify({ region: 'JP' }) }); setNotice('success', run.message); await reloadSourceData(); } catch (error) { setNotice('error', errorMessage(error)); } finally { sourceBusy.value = ''; } }
async function collectCatalog() { catalogCollecting.value = true; try { const saved = await adminApi('/collect/run', { method: 'POST', body: JSON.stringify({ force: true }) }); setNotice('success', `真实商品日报已生成，共 ${saved.products?.length || 0} 个商品`); reportSummaries.value = await api('/reports/summaries?limit=30'); } catch (error) { setNotice('error', errorMessage(error)); } finally { catalogCollecting.value = false; } }

async function saveSettings(value) { await execute(() => adminApi('/admin/settings', { method: 'PUT', body: JSON.stringify(value) }).then((saved) => { settings.value = saved; }), '配置已保存'); }
async function saveUser(row, close) { if (await execute(() => adminApi(row.id ? `/admin/users/${row.id}` : '/admin/users', { method: row.id ? 'PUT' : 'POST', body: JSON.stringify(currentTenantRow(row)) }), '用户已保存', reloadCoreLists)) close?.(); }
async function deleteUser(row) { await execute(() => adminApi(`/admin/users/${row.id}`, { method: 'DELETE' }), '用户已删除', reloadCoreLists); }
async function saveRole(row) { if (await execute(() => adminApi(row.id ? `/admin/roles/${row.id}` : '/admin/roles', { method: row.id ? 'PUT' : 'POST', body: JSON.stringify(currentTenantRow(row)) }), '角色已保存', reloadCoreLists)) editingRole.value = null; }
async function deleteRole(row) { await execute(() => adminApi(`/admin/roles/${row.id}`, { method: 'DELETE' }), '角色已删除', reloadCoreLists); }
async function saveMenu(row, close) { if (await execute(() => adminApi(row.id ? `/admin/menus/${row.id}` : '/admin/menus', { method: row.id ? 'PUT' : 'POST', body: JSON.stringify(row) }), '菜单已保存', async () => { menus.value = await adminApi('/admin/menus'); })) close?.(); }
async function deleteMenu(row) { await execute(() => adminApi(`/admin/menus/${row.id}`, { method: 'DELETE' }), '菜单已删除', async () => { menus.value = await adminApi('/admin/menus'); }); }
async function saveDictType(row, close) { if (await execute(() => adminApi(row.id ? `/admin/dict-types/${row.id}` : '/admin/dict-types', { method: row.id ? 'PUT' : 'POST', body: JSON.stringify(currentTenantRow(row)) }), '字典类型已保存', reloadCoreLists)) close?.(); }
async function deleteDictType(row) { await execute(() => adminApi(`/admin/dict-types/${row.id}`, { method: 'DELETE' }), '字典类型已删除', async () => { await reloadCoreLists(); await loadActiveData('dict'); }); }
async function saveDictData(row) { if (await execute(() => adminApi(row.id ? `/admin/dict-data/${row.id}` : '/admin/dict-data', { method: row.id ? 'PUT' : 'POST', body: JSON.stringify(currentTenantRow(row)) }), '字典数据已保存', () => loadActiveData('dict'))) editingDictData.value = null; }
async function deleteDictData(row) { await execute(() => adminApi(`/admin/dict-data/${row.id}`, { method: 'DELETE' }), '字典数据已删除', () => loadActiveData('dict')); }
async function saveConfig(row, close) { if (await execute(() => adminApi(row.id ? `/admin/configs/${row.id}` : '/admin/configs', { method: row.id ? 'PUT' : 'POST', body: JSON.stringify(currentTenantRow(row)) }), '参数已保存', reloadCoreLists)) close?.(); }
async function deleteConfig(row) { await execute(() => adminApi(`/admin/configs/${row.id}`, { method: 'DELETE' }), '参数已删除', reloadCoreLists); }
async function saveMarket(row, close) { if (await execute(() => adminApi(row.id ? `/admin/markets/${row.id}` : '/admin/markets', { method: row.id ? 'PUT' : 'POST', body: JSON.stringify(currentTenantRow(row)) }), '市场已保存', reloadCoreLists)) close?.(); }
async function deleteMarket(row) { await execute(() => adminApi(`/admin/markets/${row.id}`, { method: 'DELETE' }), '市场已删除', reloadCoreLists); }
async function saveCategory(row, close) { if (await execute(() => adminApi(row.id ? `/admin/categories/${row.id}` : '/admin/categories', { method: row.id ? 'PUT' : 'POST', body: JSON.stringify(currentTenantRow(row)) }), '品类已保存', reloadCoreLists)) close?.(); }
async function deleteCategory(row) { await execute(() => adminApi(`/admin/categories/${row.id}`, { method: 'DELETE' }), '品类已删除', reloadCoreLists); }

watch(activeMenu, (menuKey) => { loadActiveData(menuKey); });
onMounted(loadAll);

const statusOptions = ['enabled', 'disabled'];
const roleOptions = computed(() => roles.value.map((role) => ({ value: role.roleKey, label: role.roleName })));
const menuOptions = computed(() => [{ value: 0, label: '根菜单' }, ...flatMenus.value.map((menu) => ({ value: menu.id, label: menu.title }))]);
const dictTypeOptions = computed(() => dictTypes.value.map((dict) => ({ value: dict.dictType, label: dict.dictName })));
const marketOptions = computed(() => markets.value.map((market) => ({ value: market.marketKey, label: market.marketName })));
const columns = (pairs) => pairs.map(([key, label]) => ({ key, label }));
const userColumns = columns([['username', '用户名'], ['nickname', '昵称'], ['roleKey', '角色'], ['status', '状态'], ['email', '邮箱'], ['phone', '手机']]);
const roleColumns = columns([['roleKey', '标识'], ['roleName', '名称'], ['status', '状态'], ['menuKeys', '菜单权限']]);
const menuColumns = columns([['id', 'ID'], ['parentId', '父级'], ['title', '菜单'], ['menuKey', '标识'], ['path', '路径'], ['permission', '权限'], ['sortOrder', '排序'], ['status', '状态']]);
const dictTypeColumns = columns([['dictName', '名称'], ['dictType', '类型'], ['status', '状态'], ['remark', '备注']]);
const dictDataColumns = columns([['dictType', '类型'], ['dictLabel', '标签'], ['dictValue', '值'], ['sortOrder', '排序'], ['status', '状态']]);
const configColumns = columns([['configName', '名称'], ['configKey', '键'], ['configValue', '值'], ['systemBuiltin', '内置'], ['remark', '备注']]);
const marketColumns = columns([['marketKey', '标识'], ['marketName', '名称'], ['region', '区域'], ['enabled', '启用'], ['note', '说明']]);
const categoryColumns = columns([['categoryName', '品类'], ['marketKey', '市场'], ['enabled', '启用'], ['keywords', '关键词'], ['note', '说明']]);
const operLogColumns = columns([['createdAt', '时间'], ['username', '用户'], ['module', '模块'], ['action', '操作'], ['status', '状态'], ['message', '消息']]);
const loginLogColumns = columns([['createdAt', '时间'], ['username', '用户'], ['ipaddr', 'IP'], ['status', '状态'], ['message', '消息']]);
const reportColumns = columns([['reportDate', '日期'], ['title', '标题'], ['sourceMode', '数据源'], ['productCount', '商品数'], ['summary', '摘要']]);
const productColumns = columns([['rank', '排名'], ['productNameCn', '商品'], ['category', '品类'], ['sourcePlatform', '来源'], ['sourcePriceText', '源站售价'], ['heatScore', '热度'], ['estimatedProfitCny', '利润']]);
const collectionRunColumns = columns([['startedAt', '开始时间'], ['sourceKey', '数据源'], ['triggerType', '触发方式'], ['status', '状态'], ['itemCount', '条数'], ['message', '结果']]);
const userFields = computed(() => [{ key: 'username', label: '用户名' }, { key: 'password', label: '密码（编辑留空不修改）', type: 'password' }, { key: 'nickname', label: '昵称' }, { key: 'roleKey', label: '角色', type: 'select', options: roleOptions.value }, { key: 'status', label: '状态', type: 'select', options: statusOptions }, { key: 'email', label: '邮箱', type: 'email' }, { key: 'phone', label: '手机' }]);
const roleFields = computed(() => [{ key: 'roleKey', label: '角色标识' }, { key: 'roleName', label: '角色名称' }, { key: 'status', label: '状态', type: 'select', options: statusOptions }, { key: 'remark', label: '备注' }]);
const menuFields = computed(() => [{ key: 'parentId', label: '父级菜单', type: 'select', options: menuOptions.value }, { key: 'menuKey', label: '菜单标识' }, { key: 'title', label: '菜单名称' }, { key: 'icon', label: '图标' }, { key: 'path', label: '路由' }, { key: 'component', label: '组件' }, { key: 'permission', label: '权限标识' }, { key: 'sortOrder', label: '排序', type: 'number' }, { key: 'status', label: '状态', type: 'select', options: statusOptions }]);
const dictTypeFields = computed(() => [{ key: 'dictName', label: '字典名称' }, { key: 'dictType', label: '字典类型' }, { key: 'status', label: '状态', type: 'select', options: statusOptions }, { key: 'remark', label: '备注' }]);
const dictDataFields = computed(() => [{ key: 'dictType', label: '字典类型', type: 'select', options: dictTypeOptions.value }, { key: 'dictLabel', label: '标签' }, { key: 'dictValue', label: '字典值' }, { key: 'sortOrder', label: '排序', type: 'number' }, { key: 'status', label: '状态', type: 'select', options: statusOptions }, { key: 'remark', label: '备注' }]);
const configFields = computed(() => [{ key: 'configName', label: '参数名称' }, { key: 'configKey', label: '参数键' }, { key: 'configValue', label: '参数值' }, { key: 'systemBuiltin', label: '系统内置', type: 'checkbox' }, { key: 'remark', label: '备注' }]);
const marketFields = computed(() => [{ key: 'marketKey', label: '市场标识' }, { key: 'marketName', label: '市场名称' }, { key: 'region', label: '区域' }, { key: 'enabled', label: '启用', type: 'checkbox' }, { key: 'note', label: '说明' }]);
const categoryFields = computed(() => [{ key: 'categoryName', label: '品类名称' }, { key: 'marketKey', label: '市场', type: 'select', options: marketOptions.value }, { key: 'enabled', label: '启用', type: 'checkbox' }, { key: 'keywords', label: '关键词' }, { key: 'note', label: '说明' }]);
const emptyUser = { id: 0, tenantId: 'default', username: '', password: '', nickname: '', roleKey: 'operator', status: 'enabled', email: '', phone: '' };
const emptyRole = { id: 0, tenantId: 'default', roleKey: '', roleName: '', status: 'enabled', menuKeys: [], remark: '' };
const emptyMenu = { id: 0, parentId: 0, menuKey: '', title: '', icon: '', path: '', component: '', permission: '', sortOrder: 99, status: 'enabled' };
const emptyDictType = { id: 0, tenantId: 'default', dictName: '', dictType: '', status: 'enabled', remark: '' };
const emptyDictData = { id: 0, tenantId: 'default', dictType: 'sys_normal_disable', dictLabel: '', dictValue: '', sortOrder: 1, status: 'enabled', remark: '' };
const emptyConfig = { id: 0, tenantId: 'default', configName: '', configKey: '', configValue: '', systemBuiltin: false, remark: '' };
const emptyMarket = { id: 0, tenantId: 'default', marketKey: '', marketName: '', region: '', enabled: false, note: '' };
const emptyCategory = { id: 0, tenantId: 'default', categoryName: '', marketKey: 'jp', enabled: true, keywords: '', note: '' };

function iconName(icon) { return ({ system: 'settings', user: 'users', peoples: 'shield', 'tree-table': 'tree', dict: 'book', config: 'sliders', log: 'activity', shopping: 'package', link: 'database', international: 'globe', category: 'tag', time: 'clock', date: 'clock', goods: 'package', tenant: 'users' })[icon] || icon || 'circle'; }
function sourceIcon(type) { return ({ signal: 'activity', rate: 'money', catalog: 'package', history: 'chart', enrichment: 'sparkles', supplier: 'tag', social: 'activity' })[type] || 'database'; }
function sourceName(key) { return sources.value.find((source) => source.key === key)?.name || key; }
</script>
