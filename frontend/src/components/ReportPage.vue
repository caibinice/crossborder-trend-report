<template>
  <header>
    <div>
      <p class="eyebrow">TikTok Japan / Cross-border</p>
      <h1>日本热品日报</h1>
      <p class="subtitle">按热度、分类、区域、国内供货搜索链接和利润粗算辅助 TikTok 跨境选品。</p>
    </div>
    <div class="cards"><span>后端 {{ health?.status || '-' }}</span><span>模式 {{ health?.mode || '-' }}</span><span>定时 {{ health?.schedule || '-' }}</span></div>
  </header>
  <section class="toolbar">
    <div><strong>{{ report?.title || '加载中' }}</strong><p>{{ report?.summary }}</p></div>
    <div class="toolbar-actions">
      <label class="date-picker">报表日期
        <select :value="report?.reportDate || ''" @change="loadReportByDate($event.target.value)">
          <option v-for="item in reports" :key="item.id" :value="item.reportDate">{{ item.reportDate }}</option>
        </select>
      </label>
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
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue';
import ProductCard from './ProductCard.vue';
import ProductTable from './ProductTable.vue';
import { api, regionOf, searchableText } from '../lib.js';

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
</script>
