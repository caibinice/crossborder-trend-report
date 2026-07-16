<template>
  <section v-if="authEnabled && !token" class="admin-login-page">
    <form class="admin-login-card" @submit.prevent="doLogin">
      <h1>若依管理后台</h1><p>跨境选品系统</p>
      <label>账号<input v-model.trim="login.username" autocomplete="username" /></label>
      <label>密码<input v-model="login.password" type="password" autocomplete="current-password" /></label>
      <button :disabled="loggingIn">{{ loggingIn ? '登录中...' : '登录' }}</button>
      <p v-if="notice.text" :class="['notice', notice.type]">{{ notice.text }}</p>
    </form>
  </section>

  <section v-else class="ruoyi-layout">
    <aside class="ruoyi-sidebar">
      <div class="ruoyi-logo">RuoYi Admin</div>
      <nav>
        <button v-for="menu in visibleMenus" :key="menu.menuKey" :class="['ruoyi-menu', activeMenu === menu.menuKey ? 'active' : '', menu.parentId === 0 ? 'root' : 'child']" @click="selectMenu(menu)">
          <span>{{ menu.component === 'Layout' ? (expandedMenus.has(menu.menuKey) ? '⌄' : '▸') : '•' }}</span>{{ menu.title }}
        </button>
      </nav>
    </aside>

    <main class="ruoyi-main">
      <header class="ruoyi-topbar">
        <div><strong>{{ activeTitle }}</strong><small>首页 / {{ activeTitle }}</small></div>
        <div class="ruoyi-user"><span>{{ profile?.nickname || '加载中' }}（{{ profile?.tenantId || 'default' }}）</span><button v-if="authEnabled" class="ghost" @click="logout">退出</button></div>
      </header>
      <div class="ruoyi-tags"><span>首页</span><span>{{ activeTitle }}</span></div>

      <section v-if="activeMenu === 'dashboard'" class="admin-panel">
        <h2>工作台</h2>
        <div class="admin-kpis"><article><span>用户</span><b>{{ users.length }}</b></article><article><span>角色</span><b>{{ roles.length }}</b></article><article><span>菜单</span><b>{{ flatMenus.length }}</b></article><article><span>日报</span><b>{{ reportSummaries.length }}</b></article></div>
        <p class="muted">后台配置仅操作当前租户；日志、字典数据和商品池按需加载，避免进入后台即拉取完整日报明细。</p>
      </section>

      <AdminCrudPage v-if="activeMenu === 'users'" title="用户管理" description="新建用户必须设置至少 8 位初始密码；编辑时留空则保持原密码。" :rows="users" :columns="userColumns" :fields="userFields" :empty="emptyUser" @save="saveUser" @delete="deleteUser" />

      <section v-if="activeMenu === 'roles'" class="admin-panel">
        <div class="crud-head"><h2>角色管理</h2><button @click="editingRole = { ...emptyRole }">新增</button></div>
        <AdminDataTable :rows="roles" :columns="roleColumns" actions @edit="editingRole = { ...$event, menuKeys: [...($event.menuKeys || [])] }" @delete="deleteRole" />
        <AdminModalForm v-if="editingRole" title="角色表单" :model="editingRole" :fields="roleFields" @save="saveRole" @cancel="editingRole = null">
          <template #default="{ draft }"><label>菜单权限<div class="check-grid"><label v-for="menu in flatMenus" :key="menu.menuKey"><input v-model="draft.menuKeys" type="checkbox" :value="menu.menuKey" />{{ menu.title }}</label></div></label></template>
        </AdminModalForm>
      </section>

      <AdminCrudPage v-if="activeMenu === 'menus'" title="菜单管理" :rows="flatMenus" :columns="menuColumns" :fields="menuFields" :empty="emptyMenu" @save="saveMenu" @delete="deleteMenu" />

      <AdminCrudPage v-if="activeMenu === 'dict'" title="字典类型" :rows="dictTypes" :columns="dictTypeColumns" :fields="dictTypeFields" :empty="emptyDictType" @save="saveDictType" @delete="deleteDictType">
        <template #extra>
          <h3>字典数据</h3>
          <AdminDataTable :rows="dictData" :columns="dictDataColumns" actions @edit="editingDictData = { ...$event }" @delete="deleteDictData" />
          <button @click="editingDictData = { ...emptyDictData }">新增字典数据</button>
          <AdminModalForm v-if="editingDictData" title="字典数据" :model="editingDictData" :fields="dictDataFields" @save="saveDictData" @cancel="editingDictData = null" />
        </template>
      </AdminCrudPage>

      <AdminCrudPage v-if="activeMenu === 'configs'" title="参数配置" :rows="configs" :columns="configColumns" :fields="configFields" :empty="emptyConfig" @save="saveConfig" @delete="deleteConfig" />

      <section v-if="activeMenu === 'logs'" class="admin-panel"><h2>操作日志</h2><AdminDataTable :rows="operLogs" :columns="operLogColumns" /><h2>登录日志</h2><AdminDataTable :rows="loginLogs" :columns="loginLogColumns" /></section>

      <section v-if="activeMenu === 'sources'" class="admin-panel"><h2>数据源配置</h2><AdminSettingsForm v-if="settings" :settings="settings" section="sources" @save="saveSettings" /><h3>接入状态</h3><div class="source-list"><article v-for="source in sources" :key="source.name"><strong>{{ source.name }}</strong><span :class="source.configured ? 'ok' : 'warn'">{{ source.configured ? '已配置' : '未配置' }}</span><p>{{ source.useCase }}</p><a :href="source.docsUrl" target="_blank" rel="noreferrer">查看文档</a><small>{{ source.note }}</small></article></div></section>
      <AdminCrudPage v-if="activeMenu === 'markets'" title="市场配置" :rows="markets" :columns="marketColumns" :fields="marketFields" :empty="emptyMarket" @save="saveMarket" @delete="deleteMarket" />
      <AdminCrudPage v-if="activeMenu === 'categories'" title="品类配置" :rows="categories" :columns="categoryColumns" :fields="categoryFields" :empty="emptyCategory" @save="saveCategory" @delete="deleteCategory" />
      <section v-if="activeMenu === 'schedules'" class="admin-panel"><h2>采集频率配置</h2><AdminSettingsForm v-if="settings" :settings="settings" section="schedule" @save="saveSettings" /></section>
      <section v-if="activeMenu === 'dailyReports'" class="admin-panel"><h2>日报记录</h2><AdminDataTable :rows="reportSummaries" :columns="reportColumns" /></section>
      <section v-if="activeMenu === 'productPool'" class="admin-panel"><h2>商品池</h2><p v-if="!productPool.length" class="muted">暂无日报商品，先在前台生成日报。</p><AdminDataTable v-else :rows="productPool" :columns="productColumns" /></section>

      <p v-if="notice.text" :class="['notice', 'floating-notice', notice.type]">{{ notice.text }}</p>
    </main>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { api } from '../lib.js';
import AdminCrudPage from './AdminCrudPage.vue';
import AdminDataTable from './AdminDataTable.vue';
import AdminModalForm from './AdminModalForm.vue';
import AdminSettingsForm from './AdminSettingsForm.vue';

const route = useRoute();
const router = useRouter();
const token = ref(localStorage.getItem('adminToken') || '');
const login = reactive({ username: 'admin', password: 'admin' });
const authEnabled = ref(null);
const loggingIn = ref(false);
const loaded = ref(false);
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
const editingRole = ref(null);
const editingDictData = ref(null);
const expandedMenus = ref(new Set(['system', 'selection', 'reports']));
const routeMenuKeys = {
  '/admin': 'dashboard',
  '/admin/system/users': 'users',
  '/admin/system/roles': 'roles',
  '/admin/system/menus': 'menus',
  '/admin/system/dict': 'dict',
  '/admin/system/configs': 'configs',
  '/admin/system/logs': 'logs',
  '/admin/selection/sources': 'sources',
  '/admin/selection/markets': 'markets',
  '/admin/selection/categories': 'categories',
  '/admin/selection/schedules': 'schedules',
  '/admin/reports/daily': 'dailyReports',
  '/admin/reports/products': 'productPool',
};
const fallbackTitles = {
  dashboard: '首页 / 工作台', users: '用户管理', roles: '角色管理', menus: '菜单管理', dict: '字典管理', configs: '参数配置', logs: '日志管理',
  sources: '数据源配置', markets: '市场配置', categories: '品类配置', schedules: '采集频率配置', dailyReports: '日报记录', productPool: '商品池',
};

const flatMenus = computed(() => flatten(removeTenant(menus.value)));
const menuById = computed(() => new Map(flatMenus.value.map((menu) => [menu.id, menu])));
const visibleMenus = computed(() => flatMenus.value.filter((menu) => menu.parentId === 0 || expandedMenus.value.has(menuById.value.get(menu.parentId)?.menuKey)));
const activeMenu = computed(() => flatMenus.value.find((menu) => menu.path === route.path)?.menuKey || routeMenuKeys[route.path] || 'dashboard');
const activeTitle = computed(() => flatMenus.value.find((menu) => menu.menuKey === activeMenu.value)?.title || fallbackTitles[activeMenu.value] || '首页 / 工作台');

function flatten(items) { return (items || []).flatMap((menu) => [menu, ...(menu.children ? flatten(menu.children) : [])]); }
function removeTenant(items) { return (items || []).filter((menu) => menu.menuKey !== 'tenants').map((menu) => ({ ...menu, children: removeTenant(menu.children) })); }
function tenantParam() { return encodeURIComponent(profile.value?.tenantId || 'default'); }
function currentTenantRow(row) { return { ...row, tenantId: profile.value?.tenantId || 'default' }; }
function setNotice(type, text) { notice.type = type; notice.text = text || ''; }
function errorMessage(error) { return error?.message || '请求失败，请稍后重试'; }
async function adminApi(path, options = {}) { return api(path, { ...options, headers: { ...(options.headers || {}), Authorization: `Bearer ${token.value}` } }); }

function selectMenu(menu) {
  if (menu.component === 'Layout') {
    const next = new Set(expandedMenus.value);
    if (next.has(menu.menuKey)) next.delete(menu.menuKey); else next.add(menu.menuKey);
    expandedMenus.value = next;
    return;
  }
  router.push(menu.path || '/admin');
}

async function doLogin() {
  loggingIn.value = true;
  try {
    const response = await api('/admin/login', { method: 'POST', body: JSON.stringify(login) });
    token.value = response.token;
    localStorage.setItem('adminToken', response.token);
    await loadAll();
    setNotice('success', '登录成功');
  } catch (error) {
    setNotice('error', errorMessage(error));
  } finally {
    loggingIn.value = false;
  }
}

function logout() { localStorage.removeItem('adminToken'); token.value = ''; if (authEnabled.value) window.location.assign('/admin'); }

async function loadAll() {
  try {
    const status = await api('/admin/auth/status');
    authEnabled.value = status.enabled;
    if (status.enabled && !token.value) return;
    const [profileData, menuData, settingData, sourceData, summaryData] = await Promise.all([
      adminApi('/admin/profile'), adminApi('/admin/menus'), adminApi('/admin/settings'), api('/datasources'), api('/reports/summaries?limit=30'),
    ]);
    profile.value = profileData; menus.value = menuData; settings.value = settingData; sources.value = sourceData; reportSummaries.value = summaryData;
    await reloadCoreLists();
    loaded.value = true;
    await loadActiveData(activeMenu.value);
  } catch (error) {
    setNotice('error', errorMessage(error));
  }
}

async function reloadCoreLists() {
  const tenant = tenantParam();
  const [userData, roleData, dictTypeData, configData, marketData, categoryData] = await Promise.all([
    adminApi(`/admin/users?tenantId=${tenant}`), adminApi(`/admin/roles?tenantId=${tenant}`), adminApi(`/admin/dict-types?tenantId=${tenant}`),
    adminApi(`/admin/configs?tenantId=${tenant}`), adminApi(`/admin/markets?tenantId=${tenant}`), adminApi(`/admin/categories?tenantId=${tenant}`),
  ]);
  users.value = userData; roles.value = roleData; dictTypes.value = dictTypeData; configs.value = configData; markets.value = marketData; categories.value = categoryData;
}

async function loadActiveData(menuKey) {
  if (!loaded.value) return;
  const tenant = tenantParam();
  try {
    if (menuKey === 'dict') dictData.value = await adminApi(`/admin/dict-data?tenantId=${tenant}`);
    if (menuKey === 'logs') [operLogs.value, loginLogs.value] = await Promise.all([adminApi(`/admin/oper-logs?tenantId=${tenant}`), adminApi(`/admin/login-logs?tenantId=${tenant}`)]);
    if (menuKey === 'productPool') {
      const latest = reportSummaries.value[0];
      productPool.value = latest ? (await api(`/reports/${latest.id}`)).products.map((product) => ({ ...product, rank: `#${product.rank}` })) : [];
    }
  } catch (error) {
    setNotice('error', errorMessage(error));
  }
}

async function execute(work, successText, after) {
  try {
    await work();
    if (after) await after();
    setNotice('success', successText);
    return true;
  } catch (error) {
    setNotice('error', errorMessage(error));
    return false;
  }
}

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
const productColumns = columns([['rank', '排名'], ['productNameCn', '商品'], ['category', '品类'], ['heatScore', '热度'], ['estimatedProfitCny', '利润']]);
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
</script>
