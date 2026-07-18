<template>
  <main class="app-layout report-layout">
    <aside :class="['app-sidebar', { open: sidebarOpen }]">
      <div class="brand-lockup">
        <span class="brand-mark"><AppIcon name="sparkles" /></span>
        <div><strong>Northstar</strong><small>跨境趋势情报</small></div>
      </div>
      <div class="sidebar-label">市场雷达</div>
      <nav class="sidebar-nav market-nav">
        <button v-for="market in markets" :key="market.key" :class="['nav-item', { active: activeMarket === market.key }]" @click="selectMarket(market)">
          <span class="nav-icon"><AppIcon name="globe" /></span>
          <span><b>{{ market.name }}</b><small>{{ market.desc }}</small></span>
          <i :class="market.catalogEnabled ? 'live-dot' : 'signal-dot'" />
        </button>
      </nav>
      <div class="sidebar-spacer" />
      <div class="sidebar-status">
        <div><span class="pulse-dot" /><b>{{ configuredLiveSources }} 个数据能力已就绪</b></div>
        <small>公开趋势、汇率与商品目录均写入云端 MySQL</small>
      </div>
    </aside>
    <button v-if="sidebarOpen" class="sidebar-scrim" aria-label="关闭导航" @click="sidebarOpen = false" />

    <section class="app-workspace">
      <header class="app-topbar">
        <div class="topbar-title">
          <button class="icon-button mobile-menu" aria-label="打开导航" @click="sidebarOpen = true"><AppIcon name="menu" /></button>
          <div><small>趋势情报 / {{ currentMarket.name }}</small><strong>选品驾驶舱</strong></div>
        </div>
        <div class="topbar-actions">
          <span class="sync-state"><span class="pulse-dot" />数据云端同步</span>
          <ThemeToggle />
          <RouterLink class="button secondary-button" to="/admin"><AppIcon name="settings" />管理后台</RouterLink>
        </div>
      </header>

      <div class="page-container report-page">
        <section class="hero-panel">
          <div class="hero-copy">
            <span class="overline"><AppIcon name="bolt" />LIVE COMMERCE INTELLIGENCE</span>
            <h1>{{ currentMarket.name }}<br /><em>商品机会雷达</em></h1>
            <p>融合真实商品目录、搜索趋势、多币种汇率与采购成本，快速判断值得验证的跨境机会。</p>
            <div class="hero-badges"><span>Google Trends</span><span>WooCommerce Store API</span><span>Frankfurter FX</span></div>
          </div>
          <div class="hero-orbit" aria-hidden="true">
            <div class="orbit-ring ring-one" /><div class="orbit-ring ring-two" />
            <span class="orbit-core"><AppIcon name="sparkles" /></span>
            <span class="orbit-node node-a">JP</span><span class="orbit-node node-b">US</span><span class="orbit-node node-c">SEA</span>
          </div>
        </section>

        <section class="metric-grid dashboard-metrics">
          <article><span class="metric-icon blue"><AppIcon name="database" /></span><div><small>当前商品</small><b>{{ products.length }}</b><p>{{ realProductCount }} 条真实目录数据</p></div></article>
          <article><span class="metric-icon violet"><AppIcon name="activity" /></span><div><small>平均热度</small><b>{{ averageHeat }}</b><p>基于来源排序与互动</p></div></article>
          <article><span class="metric-icon green"><AppIcon name="money" /></span><div><small>正利润机会</small><b>{{ profitableCount }}</b><p>按当前费用模型估算</p></div></article>
          <article><span class="metric-icon amber"><AppIcon name="globe" /></span><div><small>JPY / CNY</small><b>{{ exchangeRate?.rateValue ? Number(exchangeRate.rateValue).toFixed(5) : '-' }}</b><p>{{ exchangeRate ? `${exchangeRate.provider} · ${exchangeRate.rateDate}` : '等待首次同步' }}</p></div></article>
        </section>

        <section class="insight-grid">
          <article class="surface-panel trend-radar-panel">
            <div class="panel-heading"><div><span class="overline">REAL-TIME SIGNALS</span><h2>{{ currentMarket.name }}搜索趋势</h2></div><span class="live-chip"><span class="pulse-dot" />实时源</span></div>
            <div v-if="trendSignals.length" class="trend-list">
              <a v-for="(signal, index) in trendSignals.slice(0, 8)" :key="signal.id" :href="signal.sourceUrl" target="_blank" rel="noreferrer" class="trend-row">
                <span class="trend-rank">{{ String(index + 1).padStart(2, '0') }}</span>
                <div><b>{{ signal.keyword }}</b><span><i :style="{ width: `${trendWidth(signal)}%` }" /></span></div>
                <strong>{{ signal.trafficLabel || signal.trafficValue }}</strong>
              </a>
            </div>
            <div v-else class="compact-empty"><AppIcon name="activity" /><div><b>还没有实时趋势</b><p>进入后台数据源配置，点击 Google Trends 的“立即同步”。</p></div></div>
          </article>

          <article class="surface-panel source-overview-panel">
            <div class="panel-heading"><div><span class="overline">DATA PIPELINE</span><h2>数据链路</h2></div><RouterLink to="/admin/selection/sources">管理</RouterLink></div>
            <div class="pipeline-list">
              <div v-for="source in primarySources" :key="source.key" class="pipeline-item"><span :class="['metric-icon', source.configured ? 'green' : 'neutral']"><AppIcon :name="sourceIcon(source.type)" /></span><div><b>{{ source.name }}</b><small>{{ source.configured ? (source.live ? '开箱即用 · 已连接' : '凭证已配置') : '等待配置材料' }}</small></div><span :class="['status-pill', source.configured ? 'success' : 'pending']">{{ source.configured ? '就绪' : '待配置' }}</span></div>
            </div>
          </article>
        </section>

        <template v-if="currentMarket.catalogEnabled">
          <section class="surface-panel report-control-panel">
            <div class="report-copy"><span class="overline">LATEST PRODUCT SNAPSHOT</span><h2>{{ report?.title || '等待首份真实商品日报' }}</h2><p>{{ report?.summary || '点击右侧采集按钮，将公开商品目录实时写入云端 MySQL。' }}</p></div>
            <div class="report-actions">
              <label class="field compact-field"><span>历史日报</span><select :value="report?.id || ''" :disabled="!reports.length || loading" @change="loadReportById(Number($event.target.value))"><option v-for="item in reports" :key="item.id" :value="item.id">{{ item.reportDate }} · {{ item.productCount }} 件</option></select></label>
              <button class="primary-button" :disabled="loading" @click="collect"><AppIcon :name="loading ? 'refresh' : 'sparkles'" :class="{ spinning: loading }" />{{ loading ? '正在采集并计算…' : '采集最新商品' }}</button>
            </div>
          </section>

          <section v-if="!report && !loading" class="empty-state large-empty"><AppIcon name="package" /><h2>还没有商品日报</h2><p>首次采集会读取真实公开目录、同步汇率并计算利润，不会静默使用 Demo。</p><button class="primary-button" @click="collect">开始首次采集</button></section>
          <template v-else-if="report">
            <section class="catalog-heading">
              <div><span class="overline">OPPORTUNITY CATALOG</span><h2>商品机会池</h2><p>更新时间 {{ formatDateTime(report.createdAt) }} · {{ products.length }} 个候选</p></div>
              <div class="segmented-control"><button :class="{ active: activeTab === 'card' }" @click="activeTab = 'card'"><AppIcon name="grid" />卡片</button><button :class="{ active: activeTab === 'table' }" @click="activeTab = 'table'"><AppIcon name="table" />列表</button></div>
            </section>

            <nav v-if="activeTab === 'card'" class="category-pills"><button v-for="category in categories" :key="category" :class="{ active: category === quickCategory }" @click="quickCategory = category">{{ category }}<span>{{ categoryCount(category) }}</span></button></nav>
            <section v-if="activeTab === 'card'" class="product-grid"><ProductCard v-for="product in cardProducts" :key="product.id" :product="product" /></section>
            <template v-else>
              <section class="filter-panel modern-filter">
                <label class="field search-field"><span>搜索商品</span><div><AppIcon name="search" /><input v-model="filters.keyword" placeholder="商品名、关键词、来源" /></div></label>
                <label class="field"><span>品类</span><select v-model="filters.category"><option v-for="category in categories" :key="category">{{ category }}</option></select></label>
                <label class="field"><span>最低热度</span><input v-model="filters.minHeat" type="number" placeholder="不限" /></label>
                <label class="field"><span>最低利润</span><input v-model="filters.minProfit" type="number" placeholder="不限" /></label>
                <label class="field"><span>排序方式</span><select v-model="filters.sortBy"><option value="rank">综合排名</option><option value="heat">热度优先</option><option value="profit">利润优先</option><option value="margin">毛利率优先</option><option value="cost">成本优先</option></select></label>
                <button class="secondary-button filter-reset" @click="resetFilters">重置</button>
              </section>
              <div class="result-summary">筛选出 <b>{{ filteredProducts.length }}</b> 个商品机会</div>
              <ProductTable :products="filteredProducts" />
            </template>
          </template>
        </template>

        <section v-else class="surface-panel market-coming-panel"><span class="metric-icon blue"><AppIcon name="globe" /></span><div><span class="overline">SIGNAL MODE</span><h2>{{ currentMarket.name }}商品目录待接入</h2><p>实时搜索趋势已经可以查看；补充对应市场的官方商品 API 凭证后，即可复用现有入库、汇率和利润模型。</p></div><RouterLink class="button primary-button" to="/admin/selection/sources">配置数据源<AppIcon name="arrow" /></RouterLink></section>
      </div>
      <p v-if="notice" class="toast error"><AppIcon name="warning" />{{ notice }}</p>
    </section>
  </main>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue';
import AppIcon from './AppIcon.vue';
import ProductCard from './ProductCard.vue';
import ProductTable from './ProductTable.vue';
import ThemeToggle from './ThemeToggle.vue';
import { api, formatDateTime, isDemoProduct, regionOf, searchableText } from '../lib.js';

const markets = [
  { key: 'jp', region: 'JP', name: '日本市场', desc: '商品目录 + 实时趋势', catalogEnabled: true },
  { key: 'us', region: 'US', name: '美国市场', desc: '实时趋势信号', catalogEnabled: false },
  { key: 'sea', region: 'SG', name: '东南亚市场', desc: '新加坡趋势信号', catalogEnabled: false },
];
const activeMarket = ref('jp');
const currentMarket = computed(() => markets.find((item) => item.key === activeMarket.value) || markets[0]);
const sidebarOpen = ref(false);
const health = ref(null);
const reports = ref([]);
const report = ref(null);
const dataSources = ref([]);
const trendSignals = ref([]);
const exchangeRate = ref(null);
const activeTab = ref('card');
const quickCategory = ref('全部');
const loading = ref(false);
const notice = ref('');
const filters = reactive({ keyword: '', category: '全部', minHeat: '', minProfit: '', maxCost: '', sortBy: 'rank' });

const products = computed(() => report.value?.products || []);
const categories = computed(() => ['全部', ...new Set(products.value.map((item) => item.category))]);
const cardProducts = computed(() => quickCategory.value === '全部' ? products.value : products.value.filter((item) => item.category === quickCategory.value));
const realProductCount = computed(() => products.value.filter((item) => !isDemoProduct(item)).length);
const profitableCount = computed(() => products.value.filter((item) => Number(item.estimatedProfitCny || 0) > 0).length);
const averageHeat = computed(() => products.value.length ? (products.value.reduce((sum, item) => sum + Number(item.heatScore || 0), 0) / products.value.length).toFixed(1) : '-');
const configuredLiveSources = computed(() => dataSources.value.filter((item) => item.configured).length);
const primarySources = computed(() => dataSources.value.filter((item) => ['google-trends', 'frankfurter', 'woocommerce', 'yahoo-shopping', 'rakuten'].includes(item.key)).slice(0, 5));
const maxTraffic = computed(() => Math.max(1, ...trendSignals.value.map((item) => Number(item.trafficValue || 0))));
const filteredProducts = computed(() => {
  const keyword = filters.keyword.trim().toLowerCase();
  const minHeat = filters.minHeat === '' ? null : Number(filters.minHeat);
  const minProfit = filters.minProfit === '' ? null : Number(filters.minProfit);
  const maxCost = filters.maxCost === '' ? null : Number(filters.maxCost);
  return products.value.filter((product) => {
    if (filters.category !== '全部' && product.category !== filters.category) return false;
    if (keyword && !searchableText(product).includes(keyword)) return false;
    if (minHeat !== null && Number(product.heatScore || 0) < minHeat) return false;
    if (minProfit !== null && Number(product.estimatedProfitCny || 0) < minProfit) return false;
    return !(maxCost !== null && Number(product.domesticCostCny || 0) > maxCost);
  }).slice().sort((a, b) => {
    if (filters.sortBy === 'heat') return Number(b.heatScore || 0) - Number(a.heatScore || 0);
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
    const [healthData, summaries, sources, signals, rate] = await Promise.all([
      api('/health'), api('/reports/summaries?limit=30'), api('/datasources'),
      optional(`/trend-signals?region=${currentMarket.value.region}&limit=20`), optional('/exchange-rates/latest?base=JPY&quote=CNY'),
    ]);
    health.value = healthData; reports.value = summaries; dataSources.value = sources; trendSignals.value = signals || []; exchangeRate.value = rate;
    report.value = summaries.length ? await api(`/reports/${summaries[0].id}`) : null;
  } catch (error) { notice.value = error.message || '加载数据失败'; }
}
async function selectMarket(market) {
  activeMarket.value = market.key; sidebarOpen.value = false; trendSignals.value = [];
  trendSignals.value = await optional(`/trend-signals?region=${market.region}&limit=20`) || [];
}
async function loadReportById(id) {
  if (!id) return; loading.value = true; notice.value = '';
  try { report.value = await api(`/reports/${id}`); quickCategory.value = '全部'; } catch (error) { notice.value = error.message || '加载日报失败'; } finally { loading.value = false; }
}
async function collect() {
  loading.value = true; notice.value = '';
  try {
    report.value = await api('/collect/run', { method: 'POST', body: JSON.stringify({ force: true }) });
    const [healthData, summaries, rate] = await Promise.all([api('/health'), api('/reports/summaries?limit=30'), optional('/exchange-rates/latest?base=JPY&quote=CNY')]);
    health.value = healthData; reports.value = summaries; exchangeRate.value = rate; quickCategory.value = '全部';
  } catch (error) { notice.value = error.message || '采集商品失败'; } finally { loading.value = false; }
}
function trendWidth(signal) { return Math.max(8, Math.round(Number(signal.trafficValue || 0) / maxTraffic.value * 100)); }
function categoryCount(category) { return category === '全部' ? products.value.length : products.value.filter((item) => item.category === category).length; }
function sourceIcon(type) { return ({ signal: 'activity', rate: 'money', catalog: 'package', history: 'chart' })[type] || 'database'; }
function resetFilters() { Object.assign(filters, { keyword: '', category: '全部', minHeat: '', minProfit: '', maxCost: '', sortBy: 'rank' }); }
onMounted(load);
</script>
