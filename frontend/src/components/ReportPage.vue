<template>
  <main class="market-shell">
    <aside class="market-sidebar">
      <div class="brand-block"><span class="brand-dot" /><div><strong>跨境选品</strong><small>Trend Intelligence</small></div></div>
      <button v-for="market in markets" :key="market.key" :class="['market-item', activeMarket === market.key ? 'active' : '', !market.enabled ? 'disabled' : '']" @click="activeMarket = market.key"><span>{{ market.name }}</span><small>{{ market.desc }}</small></button>
    </aside>
    <section class="market-main">
      <header class="market-header"><div><p class="eyebrow">TikTok / Amazon / 1688</p><h1>{{ currentMarket.name }}热品选品大屏</h1><p class="subtitle">按市场、品类、关键词、热度、利润和供货成本筛选跨境商品机会。</p></div><div class="header-actions"><RouterLink class="admin-link" to="/admin">后台管理</RouterLink></div></header>

      <template v-if="currentMarket.enabled">
        <section class="kpi-grid"><article><span>后端状态</span><b>{{ health?.status || '-' }}</b></article><article><span>数据源</span><b>{{ health?.mode || '-' }}</b></article><article><span>日报数</span><b>{{ health?.reports ?? '-' }}</b></article><article><span>商品数</span><b>{{ health?.products ?? '-' }}</b></article></section>
        <section class="toolbar hero-toolbar"><div><strong>{{ report?.title || '暂无日报' }}</strong><p>{{ report?.summary || '可在此按当前后台配置生成今日日报。' }}</p></div><div class="toolbar-actions"><label class="date-picker">报表日期<select :value="report?.id || ''" :disabled="!reports.length || loading" @change="loadReportById(Number($event.target.value))"><option v-for="item in reports" :key="item.id" :value="item.id">{{ item.reportDate }}（{{ item.productCount }} 个商品）</option></select></label><button :disabled="loading" @click="collect">{{ loading ? '生成中...' : '按当前配置生成今日日报' }}</button></div></section>

        <section v-if="!report && !loading" class="empty"><h2>暂无日报</h2><p>首次使用时请点击“按当前配置生成今日日报”，不会再由读取接口自动生成数据。</p></section>
        <template v-else-if="report">
          <div class="tabs"><button :class="activeTab === 'card' ? 'active' : ''" @click="activeTab = 'card'">卡片视图</button><button :class="activeTab === 'table' ? 'active' : ''" @click="activeTab = 'table'">筛选列表</button></div>
          <template v-if="activeTab === 'card'"><nav class="category-nav"><button v-for="category in categories" :key="category" :class="category === quickCategory ? 'active' : ''" @click="quickCategory = category">{{ category }}</button></nav><section class="grid"><ProductCard v-for="product in cardProducts" :key="product.id" :product="product" /></section></template>
          <template v-else><section class="filter-panel"><label>关键词<input v-model="filters.keyword" placeholder="搜商品名、关键词、理由、来源" /></label><label>品类<select v-model="filters.category"><option v-for="category in categories" :key="category">{{ category }}</option></select></label><label>区域<select v-model="filters.region"><option v-for="region in regions" :key="region">{{ region }}</option></select></label><label>最低热度<input v-model="filters.minHeat" type="number" /></label><label>最低利润<input v-model="filters.minProfit" type="number" /></label><label>最高国内成本<input v-model="filters.maxCost" type="number" /></label><label>排序<select v-model="filters.sortBy"><option value="rank">排名</option><option value="heat">热度</option><option value="profit">利润</option><option value="margin">毛利率</option><option value="cost">国内成本</option></select></label><button class="ghost" @click="resetFilters">重置筛选</button></section><div class="result-summary">匹配 <b>{{ filteredProducts.length }}</b> 个商品</div><ProductTable :products="filteredProducts" /></template>
        </template>
      </template>
      <section v-else class="market-placeholder"><h2>{{ currentMarket.name }}即将接入</h2><p>{{ currentMarket.placeholder }}</p><RouterLink class="admin-link" to="/admin">去后台配置数据源</RouterLink></section>
      <p v-if="notice" class="notice floating-notice error">{{ notice }}</p>
    </section>
  </main>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue';
import ProductCard from './ProductCard.vue';
import ProductTable from './ProductTable.vue';
import { api, regionOf, searchableText } from '../lib.js';

const markets = [
  { key: 'jp', name: '日本市场', desc: 'TikTok JP / Amazon JP', enabled: true, placeholder: '' },
  { key: 'us', name: '美国市场', desc: 'Amazon US / TikTok US', enabled: false, placeholder: '即将接入 Amazon US / TikTok US 数据源。' },
  { key: 'sea', name: '东南亚市场', desc: 'TikTok Shop SEA', enabled: false, placeholder: '即将接入 TikTok Shop SEA / Shopee / Lazada 数据源。' },
];
const activeMarket = ref('jp');
const currentMarket = computed(() => markets.find((item) => item.key === activeMarket.value) || markets[0]);
const health = ref(null);
const reports = ref([]);
const report = ref(null);
const activeTab = ref('card');
const quickCategory = ref('全部');
const loading = ref(false);
const notice = ref('');
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
  return products.value.filter((product) => {
    if (filters.category !== '全部' && product.category !== filters.category) return false;
    if (filters.region !== '全部' && regionOf(product) !== filters.region) return false;
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

async function load() {
  notice.value = '';
  try {
    const [healthData, summaries] = await Promise.all([api('/health'), api('/reports/summaries?limit=30')]);
    health.value = healthData;
    reports.value = summaries;
    report.value = summaries.length ? await api(`/reports/${summaries[0].id}`) : null;
  } catch (error) {
    notice.value = error.message || '加载日报失败';
  }
}
async function loadReportById(id) {
  if (!id) return;
  loading.value = true; notice.value = '';
  try { report.value = await api(`/reports/${id}`); } catch (error) { notice.value = error.message || '加载日报失败'; } finally { loading.value = false; }
}
async function collect() {
  loading.value = true; notice.value = '';
  try {
    report.value = await api('/collect/run', { method: 'POST', body: JSON.stringify({}) });
    const [healthData, summaries] = await Promise.all([api('/health'), api('/reports/summaries?limit=30')]);
    health.value = healthData; reports.value = summaries;
  } catch (error) {
    notice.value = error.message || '生成日报失败';
  } finally {
    loading.value = false;
  }
}
function resetFilters() { Object.assign(filters, { keyword: '', category: '全部', region: '全部', minHeat: '', minProfit: '', maxCost: '', sortBy: 'rank' }); }
onMounted(load);
</script>
