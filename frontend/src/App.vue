<template>
  <main>
    <div class="page-tabs">
      <a :class="['nav-button', page === 'report' ? 'active' : '']" href="/">选品报表</a>
      <a :class="['nav-button', page === 'admin' ? 'active' : '']" href="/admin">后台管理</a>
    </div>

    <AdminPage v-if="page === 'admin'" />
    <ReportPage v-else />
  </main>
</template>

<script setup>
import { computed, reactive, ref, onMounted } from 'vue';

const API = import.meta.env.VITE_API_BASE || 'http://localhost:8090/api';
const YEN = '¥';
const CNY = '￥';
const page = computed(() => window.location.pathname.startsWith('/admin') ? 'admin' : 'report');

async function api(path, options = {}) {
  const response = await fetch(`${API}${path}`, {
    headers: { 'Content-Type': 'application/json', ...(options.headers || {}) },
    ...options,
  });
  if (!response.ok) throw new Error(await response.text());
  return response.json();
}

function money(value, unit = CNY) {
  if (value === null || value === undefined || Number.isNaN(Number(value))) return '-';
  return `${unit}${Number(value).toFixed(2)}`;
}
function pct(value) { return `${(Number(value || 0) * 100).toFixed(1)}%`; }
function regionOf(product) {
  return `${product.sourcePlatform || ''} ${product.sourceUrl || ''}`.toLowerCase().match(/jp|japan|co\.jp|tiktok/) ? '日本' : '未知';
}
function searchableText(product) {
  return [
    product.productNameCn,
    product.productNameJp,
    product.keywords,
    product.category,
    product.sourcePlatform,
    product.reason,
    ...(product.domesticLinks || []).map((item) => `${item.platform} ${item.title} ${item.note}`),
  ].filter(Boolean).join(' ').toLowerCase();
}
function bestLink(product) {
  return (product.domesticLinks || []).slice().sort((a, b) => Number(a.priceCny || 0) - Number(b.priceCny || 0))[0];
}
function splitText(value) {
  return (value || '').split(/[，,\n]/).map((item) => item.trim()).filter(Boolean);
}
function joinText(value) { return (value || []).join(', '); }

const ProductCard = {
  props: ['product'],
  setup(props) { return { money, pct, regionOf, YEN, CNY, product: props.product }; },
  template: `
    <article class="product">
      <div class="top"><span class="rank">#{{ product.rank }}</span><span>{{ product.category }}</span><span>{{ regionOf(product) }}</span><strong>热度 {{ product.heatScore }}</strong></div>
      <h2>{{ product.productNameCn }}</h2>
      <p class="jp">{{ product.productNameJp }}</p>
      <p>{{ product.reason }}</p>
      <div class="metrics">
        <span>日本售价 {{ money(product.jpPriceJpy, YEN) }}</span>
        <span>折合 {{ money(product.jpPriceCny, CNY) }}</span>
        <span>国内成本 {{ money(product.domesticCostCny, CNY) }}</span>
        <span>利润 {{ money(product.estimatedProfitCny, CNY) }}</span>
        <span>毛利率 {{ pct(product.estimatedMargin) }}</span>
      </div>
      <a :href="product.sourceUrl" target="_blank" rel="noreferrer">源站/搜索趋势</a>
      <h3>国内采购候选</h3>
      <ul>
        <li v-for="link in product.domesticLinks || []" :key="link.id">
          <a :href="link.url" target="_blank" rel="noreferrer">{{ link.platform }}: {{ link.title }}</a>
          <b>{{ money(link.priceCny, CNY) }}</b>
          <small>{{ link.note }}</small>
        </li>
      </ul>
    </article>
  `,
};

const ProductTable = {
  props: ['products'],
  setup() { return { money, pct, regionOf, bestLink, YEN, CNY }; },
  template: `
    <section v-if="!products.length" class="empty">没有符合筛选条件的商品。</section>
    <section v-else class="table-wrap">
      <table>
        <thead><tr><th>排名</th><th>商品</th><th>品类</th><th>区域</th><th>热度</th><th>日本售价</th><th>国内成本</th><th>利润</th><th>毛利率</th><th>最优供货</th><th>来源</th></tr></thead>
        <tbody>
          <tr v-for="product in products" :key="product.id">
            <td>#{{ product.rank }}</td>
            <td><strong>{{ product.productNameCn }}</strong><small>{{ product.productNameJp }}</small><small>{{ product.keywords }}</small></td>
            <td>{{ product.category }}</td>
            <td>{{ regionOf(product) }}</td>
            <td>{{ product.heatScore }}</td>
            <td>{{ money(product.jpPriceJpy, YEN) }}<small>{{ money(product.jpPriceCny, CNY) }}</small></td>
            <td>{{ money(product.domesticCostCny, CNY) }}</td>
            <td :class="Number(product.estimatedProfitCny) >= 0 ? 'positive' : 'negative'">{{ money(product.estimatedProfitCny, CNY) }}</td>
            <td>{{ pct(product.estimatedMargin) }}</td>
            <td>
              <a v-if="bestLink(product)" :href="bestLink(product).url" target="_blank" rel="noreferrer">{{ bestLink(product).platform }}<small>{{ money(bestLink(product).priceCny, CNY) }}</small></a>
              <span v-else>-</span>
            </td>
            <td><a :href="product.sourceUrl" target="_blank" rel="noreferrer">{{ product.sourcePlatform }}</a></td>
          </tr>
        </tbody>
      </table>
    </section>
  `,
};

const ReportPage = {
  components: { ProductCard, ProductTable },
  setup() {
    const health = ref(null);
    const reports = ref([]);
    const report = ref(null);
    const activeTab = ref('card');
    const quickCategory = ref('全部');
    const loading = ref(false);
    const filters = reactive({ keyword: '', category: '全部', region: '全部', minHeat: '', minProfit: '', maxCost: '', sortBy: 'rank' });

    const products = computed(() => report.value?.products || []);
    const categories = computed(() => ['全部', ...new Set(products.value.map((item) => item.category))]);
    const regions = computed(() => ['全部', ...new Set(products.value.map(regionOf))]);
    const cardProducts = computed(() => quickCategory.value === '全部' ? products.value : products.value.filter((item) => item.category === quickCategory.value));
    const filteredProducts = computed(() => {
      const keyword = filters.keyword.trim().toLowerCase();
      const minHeat = filters.minHeat === '' ? null : Number(filters.minHeat);
      const minProfit = filters.minProfit === '' ? null : Number(filters.minProfit);
      const maxCost = filters.maxCost === '' ? null : Number(filters.maxCost);
      const rows = products.value.filter((product) => {
        if (filters.category !== '全部' && product.category !== filters.category) return false;
        if (filters.region !== '全部' && regionOf(product) !== filters.region) return false;
        if (keyword && !searchableText(product).includes(keyword)) return false;
        if (minHeat !== null && Number(product.heatScore || 0) < minHeat) return false;
        if (minProfit !== null && Number(product.estimatedProfitCny || 0) < minProfit) return false;
        if (maxCost !== null && Number(product.domesticCostCny || 0) > maxCost) return false;
        return true;
      });
      return rows.slice().sort((a, b) => {
        if (filters.sortBy === 'heat') return Number(b.heatScore || 0) - Number(a.heatScore || 0);
        if (filters.sortBy === 'profit') return Number(b.estimatedProfitCny || 0) - Number(a.estimatedProfitCny || 0);
        if (filters.sortBy === 'margin') return Number(b.estimatedMargin || 0) - Number(a.estimatedMargin || 0);
        if (filters.sortBy === 'cost') return Number(a.domesticCostCny || 0) - Number(b.domesticCostCny || 0);
        return Number(a.rank || 0) - Number(b.rank || 0);
      });
    });

    async function load() {
      const [healthData, reportList] = await Promise.all([api('/health'), api('/reports')]);
      health.value = healthData;
      reports.value = reportList;
      report.value = reportList[0] || await api('/reports/latest');
    }
    async function loadReportByDate(date) {
      loading.value = true;
      try { report.value = await api(`/report?date=${date}`); } finally { loading.value = false; }
    }
    async function collect() {
      loading.value = true;
      try {
        report.value = await api('/collect/run', { method: 'POST', body: JSON.stringify({}) });
        const [healthData, reportList] = await Promise.all([api('/health'), api('/reports')]);
        health.value = healthData;
        reports.value = reportList;
      } finally { loading.value = false; }
    }
    function resetFilters() {
      Object.assign(filters, { keyword: '', category: '全部', region: '全部', minHeat: '', minProfit: '', maxCost: '', sortBy: 'rank' });
    }
    onMounted(() => load().catch(console.error));
    return { health, reports, report, activeTab, quickCategory, loading, filters, categories, regions, cardProducts, filteredProducts, loadReportByDate, collect, resetFilters };
  },
  template: `
    <header>
      <div><p class="eyebrow">TikTok Japan / Cross-border</p><h1>日本热品日报</h1><p class="subtitle">按热度、分类、区域、国内供货搜索链接和利润粗算辅助 TikTok 跨境选品。</p></div>
      <div class="cards"><span>后端 {{ health?.status || '-' }}</span><span>模式 {{ health?.mode || '-' }}</span><span>定时 {{ health?.schedule || '-' }}</span></div>
    </header>
    <section class="toolbar">
      <div><strong>{{ report?.title || '加载中' }}</strong><p>{{ report?.summary }}</p></div>
      <div class="toolbar-actions">
        <label class="date-picker">报表日期<select :value="report?.reportDate || ''" @change="loadReportByDate($event.target.value)"><option v-for="item in reports" :key="item.id" :value="item.reportDate">{{ item.reportDate }}</option></select></label>
        <button @click="collect" :disabled="loading">{{ loading ? '生成中...' : '按当前配置生成今日日报' }}</button>
      </div>
    </section>
    <div class="tabs"><button :class="activeTab === 'card' ? 'active' : ''" @click="activeTab = 'card'">卡片视图</button><button :class="activeTab === 'table' ? 'active' : ''" @click="activeTab = 'table'">筛选列表</button></div>
    <template v-if="activeTab === 'card'">
      <nav class="category-nav"><button v-for="category in categories" :key="category" :class="category === quickCategory ? 'active' : ''" @click="quickCategory = category">{{ category }}</button></nav>
      <section class="grid"><ProductCard v-for="product in cardProducts" :key="product.id" :product="product" /></section>
    </template>
    <template v-else>
      <section class="filter-panel">
        <label>关键词<input v-model="filters.keyword" placeholder="搜商品名、关键词、理由、来源" /></label>
        <label>品类<select v-model="filters.category"><option v-for="category in categories" :key="category">{{ category }}</option></select></label>
        <label>区域<select v-model="filters.region"><option v-for="region in regions" :key="region">{{ region }}</option></select></label>
        <label>最低热度<input type="number" v-model="filters.minHeat" /></label>
        <label>最低利润<input type="number" v-model="filters.minProfit" /></label>
        <label>最高国内成本<input type="number" v-model="filters.maxCost" /></label>
        <label>排序<select v-model="filters.sortBy"><option value="rank">排名</option><option value="heat">热度</option><option value="profit">利润</option><option value="margin">毛利率</option><option value="cost">国内成本</option></select></label>
        <button class="ghost" @click="resetFilters">重置筛选</button>
      </section>
      <div class="result-summary">匹配 <b>{{ filteredProducts.length }}</b> 个商品</div>
      <ProductTable :products="filteredProducts" />
    </template>
  `,
};

const AdminPage = {
  setup() {
    const token = ref(localStorage.getItem('adminToken') || '');
    const login = reactive({ username: 'admin', password: 'admin' });
    const settings = ref(null);
    const sources = ref([]);
    const message = ref('');

    async function adminApi(path, options = {}) {
      return api(path, { ...options, headers: { ...(options.headers || {}), Authorization: `Bearer ${token.value}` } });
    }
    async function doLogin() {
      message.value = '';
      try {
        const data = await api('/admin/login', { method: 'POST', body: JSON.stringify(login) });
        token.value = data.token;
        localStorage.setItem('adminToken', data.token);
        message.value = '登录成功';
        await load();
      } catch (error) { message.value = '账号或密码错误'; }
    }
    async function load() {
      if (!token.value) return;
      const [settingsData, sourceData] = await Promise.all([adminApi('/admin/settings'), api('/datasources')]);
      settings.value = settingsData;
      sources.value = sourceData;
    }
    async function save() {
      try {
        settings.value = await adminApi('/admin/settings', { method: 'PUT', body: JSON.stringify(settings.value) });
        message.value = '配置已保存，动态定时任务会按新 Cron 触发；点击报表页按钮可立即按新配置生成。';
      } catch (error) { message.value = error.message; }
    }
    function logout() {
      localStorage.removeItem('adminToken');
      token.value = '';
      settings.value = null;
    }
    onMounted(() => load().catch((error) => { message.value = error.message; }));
    return { token, login, settings, sources, message, doLogin, save, logout, splitText, joinText };
  },
  template: `
    <section v-if="!token" class="admin-card">
      <h1>后台管理登录</h1><p>默认账号密码：admin / admin</p>
      <form class="login" @submit.prevent="doLogin"><label>账号<input v-model="login.username" /></label><label>密码<input type="password" v-model="login.password" /></label><button>登录</button></form>
      <p v-if="message" class="notice">{{ message }}</p>
    </section>
    <section v-else-if="!settings" class="admin-card">加载后台配置中...</section>
    <section v-else class="admin">
      <div class="admin-head"><div><p class="eyebrow">Admin Console</p><h1>数据源与检索配置</h1><p>配置国内/国外数据源、商品检索品类、区域、检索频率和利润估算参数。</p></div><button class="ghost" @click="logout">退出登录</button></div>
      <section class="admin-grid">
        <label>国外检索数据源<textarea rows="4" :value="joinText(settings.foreignSources)" @input="settings.foreignSources = splitText($event.target.value)" /><small>例如：TikTok/Apify, Amazon/Rainforest, Amazon/Keepa</small></label>
        <label>国内检索数据源<textarea rows="4" :value="joinText(settings.domesticSources)" @input="settings.domesticSources = splitText($event.target.value)" /><small>例如：1688, Taobao, Pinduoduo</small></label>
        <label>商品检索品类<textarea rows="5" :value="joinText(settings.categories)" @input="settings.categories = splitText($event.target.value)" /><small>逗号或换行分隔；报表会按这些品类过滤。</small></label>
        <label>检索区域<textarea rows="5" :value="joinText(settings.regions)" @input="settings.regions = splitText($event.target.value)" /><small>例如：日本、美国、东南亚；当前 demo 主要生成日本。</small></label>
        <label>检索频率 Cron<input v-model="settings.frequencyCron" /><small>Spring cron，例如每天 08:30：0 30 8 * * *</small></label>
        <label>每日报表商品数<input type="number" v-model.number="settings.maxProducts" /></label>
        <label>日元汇率<input type="number" step="0.001" v-model="settings.jpyCnyRate" /></label>
        <label>默认物流成本<input type="number" v-model="settings.defaultShippingCny" /></label>
        <label class="check"><input type="checkbox" v-model="settings.smartMode" />启用智能模式：自动按利润和热度综合排序</label>
      </section>
      <button @click="save">保存配置</button><p v-if="message" class="notice">{{ message }}</p>
      <h2>数据源接入状态</h2>
      <section class="source-list"><article v-for="source in sources" :key="source.name"><strong>{{ source.name }}</strong><span :class="source.configured ? 'ok' : 'warn'">{{ source.configured ? '已配置' : '未配置' }}</span><p>{{ source.useCase }}</p><a :href="source.docsUrl" target="_blank" rel="noreferrer">查看文档</a><small>{{ source.note }}</small></article></section>
    </section>
  `,
};
</script>
