<template>
  <main class="app-layout report-layout">
    <aside :class="['app-sidebar', { open: sidebarOpen }]">
      <div class="brand-lockup">
        <span class="brand-mark"><AppIcon name="sparkles" /></span>
        <div><strong>Northstar</strong><small>{{ tr('跨境趋势情报', 'Cross-border Intelligence') }}</small></div>
      </div>
      <div class="sidebar-label">{{ tr('市场雷达', 'MARKET RADAR') }}</div>
      <nav class="sidebar-nav market-nav">
        <button v-for="market in markets" :key="market.key" :class="['nav-item', { active: activeMarket === market.key }]" @click="selectMarket(market)">
          <span class="nav-icon"><AppIcon name="globe" /></span>
          <span><b>{{ marketText(market, 'name') }}</b><small>{{ marketText(market, 'desc') }}</small></span>
          <i :class="market.catalogEnabled ? 'live-dot' : 'signal-dot'" />
        </button>
      </nav>
      <div class="sidebar-spacer" />
      <div class="sidebar-status">
        <div><span class="pulse-dot" /><b>{{ configuredLiveSources }} {{ tr('个数据能力已就绪', 'data capabilities ready') }}</b></div>
        <small>{{ tr('公开趋势、汇率与商品目录均写入云端 MySQL', 'Public trends, FX, and catalogs are persisted to MySQL') }}</small>
      </div>
    </aside>
    <button v-if="sidebarOpen" class="sidebar-scrim" :aria-label="tr('关闭导航', 'Close navigation')" @click="sidebarOpen = false" />

    <section class="app-workspace">
      <header class="app-topbar">
        <div class="topbar-title">
          <button class="icon-button mobile-menu" :aria-label="tr('打开导航', 'Open navigation')" @click="sidebarOpen = true"><AppIcon name="menu" /></button>
          <div><small>{{ tr('趋势情报', 'Trend Intelligence') }} / {{ currentMarketName }}</small><strong>{{ tr('选品驾驶舱', 'Product Discovery') }}</strong></div>
        </div>
        <div class="topbar-actions">
          <span class="sync-state"><span class="pulse-dot" />{{ tr('数据云端同步', 'Cloud data synced') }}</span>
          <ThemeToggle :english="isEnglish" />
          <RouterLink class="button secondary-button" to="/admin"><AppIcon name="settings" />{{ tr('管理后台', 'Admin') }}</RouterLink>
        </div>
      </header>

      <div class="page-container report-page">
        <section class="hero-panel">
          <div class="hero-copy">
            <span class="overline"><AppIcon name="bolt" />LIVE COMMERCE INTELLIGENCE</span>
            <h1>{{ currentMarketName }}<br /><em>{{ tr('商品机会雷达', 'Product Opportunity Radar') }}</em></h1>
            <p>{{ tr('融合真实商品目录、搜索趋势、多币种汇率与采购成本，快速判断值得验证的跨境机会。', 'Combine live product catalogs, search trends, multi-currency FX, and sourcing costs to identify opportunities worth validating.') }}</p>
            <div class="hero-badges"><span>Google Trends</span><span>Rakuten Ichiba</span><span>WooCommerce</span><span>DeepSeek V4 Pro</span></div>
          </div>
          <div class="hero-orbit" aria-hidden="true">
            <div class="orbit-ring ring-one" /><div class="orbit-ring ring-two" />
            <span class="orbit-core"><AppIcon name="sparkles" /></span>
            <span class="orbit-node node-a">JP</span><span class="orbit-node node-b">US</span><span class="orbit-node node-c">SEA</span>
          </div>
        </section>

        <section class="metric-grid dashboard-metrics">
          <article><span class="metric-icon blue"><AppIcon name="database" /></span><div><small>{{ tr('当前商品', 'Current products') }}</small><b>{{ products.length }}</b><p>{{ realProductCount }} {{ tr('条真实目录数据', 'live catalog records') }}</p></div></article>
          <article><span class="metric-icon violet"><AppIcon name="activity" /></span><div><small>{{ tr('平均热度', 'Average heat') }}</small><b>{{ averageHeat }}</b><p>{{ tr('基于来源排序与互动', 'Based on source rank and engagement') }}</p></div></article>
          <article><span class="metric-icon green"><AppIcon name="money" /></span><div><small>{{ tr('正利润机会', 'Positive-margin opportunities') }}</small><b>{{ profitableCount }}</b><p>{{ tr('按当前费用模型估算', 'Estimated with the current cost model') }}</p></div></article>
          <article><span class="metric-icon amber"><AppIcon name="globe" /></span><div><small>{{ currentMarket.currency }} / CNY</small><b>{{ exchangeRate?.rateValue ? Number(exchangeRate.rateValue).toFixed(5) : '-' }}</b><p>{{ exchangeRate ? `${exchangeRate.provider} · ${exchangeRate.rateDate}` : tr('等待首次同步', 'Waiting for first sync') }}</p></div></article>
        </section>

        <section class="insight-grid">
          <article class="surface-panel trend-radar-panel">
            <div class="panel-heading"><div><span class="overline">REAL-TIME SIGNALS</span><h2>{{ currentMarketName }}{{ tr('搜索趋势', ' Search Trends') }}</h2></div><span class="live-chip"><span class="pulse-dot" />{{ tr('实时源', 'Live feed') }}</span></div>
            <div v-if="trendSignals.length" class="trend-list">
              <a v-for="(signal, index) in sortedTrendSignals.slice(0, 8)" :key="signal.id" :href="signal.sourceUrl" target="_blank" rel="noreferrer" class="trend-row" :title="`${tr('原始搜索量：', 'Original search traffic: ')}${signal.trafficLabel || signal.trafficValue}`">
                <span class="trend-rank">{{ String(index + 1).padStart(2, '0') }}</span>
                <div><b>{{ signal.keyword }}</b><span><i :style="{ width: `${trendHeat(signal, index)}%` }" /></span></div>
                <strong>{{ tr('热度', 'Heat') }} {{ trendHeat(signal, index) }}</strong>
              </a>
            </div>
            <div v-else class="compact-empty"><AppIcon name="activity" /><div><b>{{ tr('还没有实时趋势', 'No live trends yet') }}</b><p>{{ tr('进入后台数据源配置，点击 Google Trends 的“立即同步”。', 'Open Data Sources in Admin and sync Google Trends.') }}</p></div></div>
          </article>

          <article class="surface-panel source-overview-panel">
            <div class="panel-heading"><div><span class="overline">DATA PIPELINE</span><h2>{{ tr('数据链路', 'Data Pipeline') }}</h2></div><RouterLink to="/admin/selection/sources">{{ tr('管理', 'Manage') }}</RouterLink></div>
            <div class="pipeline-list">
              <div v-for="source in primarySources" :key="source.key" class="pipeline-item"><span :class="['metric-icon', source.configured ? 'green' : 'neutral']"><AppIcon :name="sourceIcon(source.type)" /></span><div><b>{{ source.name }}</b><small>{{ source.configured ? (source.live ? tr('开箱即用 · 已连接', 'Public · Connected') : tr('凭证已配置', 'Credentials configured')) : tr('等待配置材料', 'Configuration required') }}</small></div><span :class="['status-pill', source.configured ? 'success' : 'pending']">{{ source.configured ? tr('就绪', 'Ready') : tr('待配置', 'Pending') }}</span></div>
            </div>
          </article>
        </section>

        <template v-if="currentMarket.catalogEnabled">
          <section class="surface-panel report-control-panel">
            <div class="report-copy"><span class="overline">LATEST PRODUCT SNAPSHOT</span><h2>{{ report?.title || tr('等待首份真实商品日报', 'Waiting for the first live product report') }}</h2><p>{{ report?.summary || tr('点击右侧采集按钮，将公开商品目录实时写入云端 MySQL。', 'Collect public catalog products and persist them to MySQL.') }}</p></div>
            <div class="report-actions">
              <label class="field compact-field"><span>{{ tr('历史日报', 'Report history') }}</span><select :value="report?.id || ''" :disabled="!reports.length || loading" @change="loadReportById(Number($event.target.value))"><option v-for="item in reports" :key="item.id" :value="item.id">{{ item.reportDate }} · {{ item.productCount }} {{ tr('件', 'products') }}</option></select></label>
              <button class="primary-button" :disabled="loading" @click="collect"><AppIcon :name="loading ? 'refresh' : 'sparkles'" :class="{ spinning: loading }" />{{ loading ? tr('正在采集并计算…', 'Collecting and scoring…') : tr('采集最新商品', 'Collect latest products') }}</button>
            </div>
          </section>

          <section v-if="!report && !loading" class="empty-state large-empty"><AppIcon name="package" /><h2>{{ tr('还没有商品日报', 'No product report yet') }}</h2><p>{{ tr('首次采集会读取真实公开目录、同步汇率并计算利润，不会静默使用 Demo。', 'The first run reads live public catalogs, resolves FX, and estimates margins without silently using demo data.') }}</p><button class="primary-button" @click="collect">{{ tr('开始首次采集', 'Start first collection') }}</button></section>
          <template v-else-if="report">
            <section class="catalog-heading">
              <div><span class="overline">OPPORTUNITY CATALOG</span><h2>{{ tr('商品机会池', 'Product Opportunity Pool') }}</h2><p>{{ tr('更新时间', 'Updated') }} {{ formatDateTime(report.createdAt, locale) }} · {{ products.length }} {{ tr('个候选', 'candidates') }}</p></div>
              <div class="catalog-controls">
                <div class="segmented-control ranking-switch"><button :class="{ active: rankingView === 'heat' }" @click="rankingView = 'heat'">{{ tr('综合热度', 'Composite heat') }}</button><button :class="{ active: rankingView === 'volume' }" @click="rankingView = 'volume'">{{ tr('销量指数', 'Sales-volume proxy') }}</button><button :class="{ active: rankingView === 'amount' }" @click="rankingView = 'amount'">{{ tr('销售额指数', 'Sales-value proxy') }}</button></div>
                <div class="segmented-control"><button :class="{ active: activeTab === 'card' }" @click="activeTab = 'card'"><AppIcon name="grid" />{{ tr('卡片', 'Cards') }}</button><button :class="{ active: activeTab === 'table' }" @click="activeTab = 'table'"><AppIcon name="table" />{{ tr('列表', 'Table') }}</button></div>
              </div>
            </section>

            <nav v-if="activeTab === 'card'" class="category-pills"><button v-for="category in categories" :key="category" :class="{ active: category === quickCategory }" @click="quickCategory = category">{{ category }}<span>{{ categoryCount(category) }}</span></button></nav>
            <section v-if="activeTab === 'card'" class="product-grid"><ProductCard v-for="(product, index) in cardProducts" :key="product.id" :product="product" :display-rank="index + 1" :english="isEnglish" /></section>
            <template v-else>
              <section class="filter-panel modern-filter">
                <label class="field search-field"><span>{{ tr('搜索商品', 'Search products') }}</span><div><AppIcon name="search" /><input v-model="filters.keyword" :placeholder="tr('商品名、关键词、来源', 'Name, keyword, or source')" /></div></label>
                <label class="field"><span>{{ tr('品类', 'Category') }}</span><select v-model="filters.category"><option v-for="category in categories" :key="category">{{ category }}</option></select></label>
                <label class="field"><span>{{ tr('最低热度', 'Minimum heat') }}</span><input v-model="filters.minHeat" type="number" :placeholder="tr('不限', 'Any')" /></label>
                <label class="field"><span>{{ tr('最低利润', 'Minimum profit') }}</span><input v-model="filters.minProfit" type="number" :placeholder="tr('不限', 'Any')" /></label>
                <label class="field"><span>{{ tr('排序方式', 'Sort by') }}</span><select v-model="filters.sortBy"><option value="rank">{{ tr('综合排名', 'Composite rank') }}</option><option value="heat">{{ tr('热度优先', 'Heat') }}</option><option value="volume">{{ tr('销量指数', 'Sales-volume proxy') }}</option><option value="amount">{{ tr('销售额指数', 'Sales-value proxy') }}</option><option value="profit">{{ tr('利润优先', 'Profit') }}</option><option value="margin">{{ tr('毛利率优先', 'Margin') }}</option><option value="cost">{{ tr('成本优先', 'Cost') }}</option></select></label>
                <button class="secondary-button filter-reset" @click="resetFilters">{{ tr('重置', 'Reset') }}</button>
              </section>
              <div class="result-summary">{{ tr('筛选出', '') }} <b>{{ filteredProducts.length }}</b> {{ tr('个商品机会', 'product opportunities') }}</div>
              <ProductTable :products="filteredProducts" :english="isEnglish" />
            </template>
          </template>
        </template>

        <section v-else class="surface-panel market-coming-panel"><span class="metric-icon blue"><AppIcon name="globe" /></span><div><span class="overline">SIGNAL MODE</span><h2>{{ currentMarketName }}{{ tr('商品目录待接入', ' catalog pending') }}</h2><p>{{ tr('实时搜索趋势已经可以查看；补充对应市场的官方商品 API 凭证后，即可复用现有入库、汇率和利润模型。', 'Live search trends are available. Configure a catalog source to enable product reports and margin analysis.') }}</p></div><RouterLink class="button primary-button" to="/admin/selection/sources">{{ tr('配置数据源', 'Configure sources') }}<AppIcon name="arrow" /></RouterLink></section>
      </div>
      <p v-if="notice" class="toast error"><AppIcon name="warning" />{{ notice }}</p>
    </section>
    <div v-if="actionAuthOpen" class="modal-backdrop" role="presentation" @mousedown.self="closeActionAuth">
      <form class="modal-dialog action-auth-dialog" role="dialog" aria-modal="true" aria-labelledby="action-auth-title" @submit.prevent="verifyAndCollect">
        <header class="modal-header"><div><span class="overline">PROTECTED ACTION</span><h3 id="action-auth-title">{{ tr('验证敏感操作', 'Verify protected action') }}</h3></div><button type="button" class="icon-button" :aria-label="tr('关闭', 'Close')" @click="closeActionAuth"><AppIcon name="x" /></button></header>
        <div class="modal-scroll">
          <p class="action-auth-copy">{{ tr('实时抓取、AI 补全与报告生成会消耗服务器和外部接口资源，请输入操作密码继续。', 'Live collection, AI enrichment, and report generation consume server and upstream resources. Enter the action password to continue.') }}</p>
          <label class="field"><span>{{ tr('操作密码', 'Action password') }}</span><input v-model="actionPassword" autofocus type="password" autocomplete="current-password" /></label>
          <p v-if="actionAuthError" class="inline-notice error">{{ actionAuthError }}</p>
        </div>
        <footer class="modal-actions"><button type="button" class="secondary-button" :disabled="actionVerifying" @click="closeActionAuth">{{ tr('取消', 'Cancel') }}</button><button class="primary-button" :disabled="actionVerifying || !actionPassword"><AppIcon :name="actionVerifying ? 'refresh' : 'shield'" :class="{ spinning: actionVerifying }" />{{ actionVerifying ? tr('验证中…', 'Verifying…') : tr('验证并采集', 'Verify and collect') }}</button></footer>
      </form>
    </div>
  </main>
</template>

<script setup>
import { computed, onMounted, onUnmounted, reactive, ref, watch } from 'vue';
import AppIcon from './AppIcon.vue';
import ProductCard from './ProductCard.vue';
import ProductTable from './ProductTable.vue';
import ThemeToggle from './ThemeToggle.vue';
import { api, formatDateTime, isDemoProduct, searchableText } from '../lib.js';

const markets = [
  { key: 'jp', region: 'JP', currency: 'JPY', nameZh: '日本市场', nameEn: 'Japan', descZh: '商品目录 + 实时趋势', descEn: 'Public catalogs + live trends', catalogEnabled: true },
  { key: 'us', region: 'US', currency: 'USD', nameZh: '美国市场', nameEn: 'United States', descZh: '实时趋势信号', descEn: 'US catalogs + live trends', catalogEnabled: true },
  { key: 'sea', region: 'SG', currency: 'SGD', nameZh: '东南亚市场', nameEn: 'Southeast Asia', descZh: '新加坡趋势信号', descEn: 'Singapore catalogs + SEA trends', catalogEnabled: true },
];
const activeMarket = ref('jp');
const currentMarket = computed(() => markets.find((item) => item.key === activeMarket.value) || markets[0]);
const isEnglish = computed(() => currentMarket.value.key !== 'jp');
const currentMarketName = computed(() => marketText(currentMarket.value, 'name'));
const locale = computed(() => isEnglish.value ? 'en-US' : 'zh-CN');
const allCategory = computed(() => isEnglish.value ? 'All' : '全部');
const sidebarOpen = ref(false);
const health = ref(null);
const reports = ref([]);
const report = ref(null);
const dataSources = ref([]);
const trendSignals = ref([]);
const exchangeRate = ref(null);
const activeTab = ref('card');
const rankingView = ref('heat');
const quickCategory = ref('全部');
const loading = ref(false);
const notice = ref('');
const actionAuthOpen = ref(false);
const actionPassword = ref('');
const actionAuthError = ref('');
const actionVerifying = ref(false);
const filters = reactive({ keyword: '', category: '全部', minHeat: '', minProfit: '', maxCost: '', sortBy: 'rank' });
const ACTION_TOKEN_KEY = 'crossborder-action-token';

const products = computed(() => report.value?.products || []);
const categories = computed(() => [allCategory.value, ...new Set(products.value.map((item) => item.category))]);
const rankedProducts = computed(() => products.value.slice().sort((a, b) => {
  const field = rankingView.value === 'volume' ? 'salesVolumeScore' : rankingView.value === 'amount' ? 'salesAmountScore' : 'heatScore';
  return Number(b[field] || 0) - Number(a[field] || 0) || Number(b.heatScore || 0) - Number(a.heatScore || 0);
}));
const cardProducts = computed(() => quickCategory.value === allCategory.value ? rankedProducts.value : rankedProducts.value.filter((item) => item.category === quickCategory.value));
const realProductCount = computed(() => products.value.filter((item) => !isDemoProduct(item)).length);
const profitableCount = computed(() => products.value.filter((item) => Number(item.estimatedProfitCny || 0) > 0).length);
const averageHeat = computed(() => products.value.length ? (products.value.reduce((sum, item) => sum + Number(item.heatScore || 0), 0) / products.value.length).toFixed(1) : '-');
const configuredLiveSources = computed(() => dataSources.value.filter((item) => item.configured).length);
const primarySources = computed(() => {
  const keys = currentMarket.value.key === 'jp'
    ? ['google-trends', 'frankfurter', 'woocommerce', 'yahoo-shopping', 'rakuten']
    : ['google-trends', 'frankfurter', 'woocommerce', 'deepseek'];
  return dataSources.value.filter((item) => keys.includes(item.key)).slice(0, 5);
});
const sortedTrendSignals = computed(() => trendSignals.value.slice().sort((a, b) => Number(b.trafficValue || 0) - Number(a.trafficValue || 0)));
const filteredProducts = computed(() => {
  const keyword = filters.keyword.trim().toLowerCase();
  const minHeat = filters.minHeat === '' ? null : Number(filters.minHeat);
  const minProfit = filters.minProfit === '' ? null : Number(filters.minProfit);
  const maxCost = filters.maxCost === '' ? null : Number(filters.maxCost);
  return products.value.filter((product) => {
    if (filters.category !== allCategory.value && product.category !== filters.category) return false;
    if (keyword && !searchableText(product).includes(keyword)) return false;
    if (minHeat !== null && Number(product.heatScore || 0) < minHeat) return false;
    if (minProfit !== null && Number(product.estimatedProfitCny || 0) < minProfit) return false;
    return !(maxCost !== null && Number(product.domesticCostCny || 0) > maxCost);
  }).slice().sort((a, b) => {
    if (filters.sortBy === 'heat') return Number(b.heatScore || 0) - Number(a.heatScore || 0);
    if (filters.sortBy === 'volume') return Number(b.salesVolumeScore || 0) - Number(a.salesVolumeScore || 0);
    if (filters.sortBy === 'amount') return Number(b.salesAmountScore || 0) - Number(a.salesAmountScore || 0);
    if (filters.sortBy === 'profit') return Number(b.estimatedProfitCny || 0) - Number(a.estimatedProfitCny || 0);
    if (filters.sortBy === 'margin') return Number(b.estimatedMargin || 0) - Number(a.estimatedMargin || 0);
    if (filters.sortBy === 'cost') return Number(a.domesticCostCny || 0) - Number(b.domesticCostCny || 0);
    return Number(a.rank || 0) - Number(b.rank || 0);
  });
});

async function optional(path) { try { return await api(path); } catch { return null; } }
async function load() {
  notice.value = '';
  try {
    health.value = await api('/health');
    await loadMarketData();
  } catch (error) { notice.value = error.message || tr('加载数据失败', 'Failed to load data'); }
}
async function selectMarket(market) {
  activeMarket.value = market.key;
  sidebarOpen.value = false;
  await loadMarketData();
}
async function loadMarketData() {
  loading.value = true;
  notice.value = '';
  report.value = null;
  reports.value = [];
  trendSignals.value = [];
  quickCategory.value = allCategory.value;
  filters.category = allCategory.value;
  try {
    const market = currentMarket.value;
    const [summaries, sources, signals, rate] = await Promise.all([
      api(`/reports/summaries?limit=30&marketKey=${market.key}`),
      api(`/datasources?marketKey=${market.key}`),
      optional(`/trend-signals?region=${market.region}&limit=20`),
      optional(`/exchange-rates/latest?base=${market.currency}&quote=CNY`),
    ]);
    reports.value = summaries;
    dataSources.value = sources;
    trendSignals.value = signals || [];
    exchangeRate.value = rate;
    report.value = summaries.length ? await api(`/reports/${summaries[0].id}`) : null;
  } catch (error) {
    notice.value = error.message || tr('加载市场数据失败', 'Failed to load market data');
  } finally {
    loading.value = false;
  }
}
async function loadReportById(id) {
  if (!id) return; loading.value = true; notice.value = '';
  try { report.value = await api(`/reports/${id}`); quickCategory.value = allCategory.value; } catch (error) { notice.value = error.message || tr('加载日报失败', 'Failed to load report'); } finally { loading.value = false; }
}
function closeActionAuth() {
  if (actionVerifying.value) return;
  actionAuthOpen.value = false;
  actionPassword.value = '';
  actionAuthError.value = '';
}
async function collect() {
  const token = sessionStorage.getItem(ACTION_TOKEN_KEY);
  if (token) {
    await collectWithAuthorization(`Bearer ${token}`);
    return;
  }
  actionPassword.value = '';
  actionAuthError.value = '';
  actionAuthOpen.value = true;
}
async function verifyAndCollect() {
  actionVerifying.value = true; actionAuthError.value = '';
  try {
    const verified = await api('/action-auth/verify', { method: 'POST', body: JSON.stringify({ password: actionPassword.value }) });
    sessionStorage.setItem(ACTION_TOKEN_KEY, verified.token);
    actionAuthOpen.value = false; actionPassword.value = '';
    await collectWithAuthorization(`Bearer ${verified.token}`);
  } catch (error) {
    actionAuthError.value = error.message || tr('操作密码验证失败', 'Action password verification failed');
  } finally {
    actionVerifying.value = false;
  }
}
async function collectWithAuthorization(authorization) {
  loading.value = true; notice.value = '';
  try {
    const market = currentMarket.value;
    report.value = await api('/collect/run', {
      method: 'POST',
      headers: { Authorization: authorization },
      body: JSON.stringify({ force: true, marketKey: market.key }),
    });
    const [healthData, summaries, rate] = await Promise.all([
      api('/health'),
      api(`/reports/summaries?limit=30&marketKey=${market.key}`),
      optional(`/exchange-rates/latest?base=${market.currency}&quote=CNY`),
    ]);
    health.value = healthData; reports.value = summaries; exchangeRate.value = rate; quickCategory.value = allCategory.value;
  } catch (error) {
    if (error.status === 401) sessionStorage.removeItem(ACTION_TOKEN_KEY);
    notice.value = error.message || tr('采集商品失败', 'Product collection failed');
  } finally { loading.value = false; }
}
function trendHeat(signal, index = 0) {
  const values = sortedTrendSignals.value.map((item) => Math.log10(Math.max(0, Number(item.trafficValue || 0)) + 1));
  if (!values.length) return 1;
  const max = Math.max(...values); const min = Math.min(...values); const current = Math.log10(Math.max(0, Number(signal.trafficValue || 0)) + 1);
  if (max === min) return Math.max(1, Math.round(100 - index * 99 / Math.max(1, values.length - 1)));
  return Math.max(1, Math.min(100, Math.round(1 + (current - min) / (max - min) * 99)));
}
function categoryCount(category) { return category === allCategory.value ? products.value.length : products.value.filter((item) => item.category === category).length; }
function sourceIcon(type) { return ({ signal: 'activity', rate: 'money', catalog: 'package', history: 'chart' })[type] || 'database'; }
function resetFilters() { Object.assign(filters, { keyword: '', category: allCategory.value, minHeat: '', minProfit: '', maxCost: '', sortBy: 'rank' }); }
function tr(chinese, english) { return isEnglish.value ? english : chinese; }
function marketText(market, field) { return isEnglish.value ? market[`${field}En`] : market[`${field}Zh`]; }
watch(isEnglish, (english) => {
  document.documentElement.lang = english ? 'en' : 'zh-CN';
  document.title = english ? 'Northstar Cross-border Intelligence' : 'Northstar 跨境趋势情报';
}, { immediate: true });
onUnmounted(() => {
  document.documentElement.lang = 'zh-CN';
  document.title = 'Northstar 跨境趋势情报';
});
onMounted(load);
</script>
