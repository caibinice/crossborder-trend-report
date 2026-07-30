<template>
  <section v-if="!products.length" class="empty-state"><AppIcon name="search" /><h3>{{ tr('没有匹配商品', 'No matching products') }}</h3><p>{{ tr('调整关键词、成本或利润筛选后再试。', 'Adjust the keyword, cost, or profit filters and try again.') }}</p></section>
  <section v-else class="data-table-shell product-table-shell">
    <table>
      <thead><tr><th>{{ tr('商品机会', 'Product opportunity') }}</th><th>{{ tr('来源', 'Source') }}</th><th>{{ tr('热度', 'Heat') }}</th><th>{{ tr('源站售价', 'Source price') }}</th><th>{{ tr('国内成本', 'Sourcing cost') }}</th><th>{{ tr('预估利润', 'Estimated profit') }}</th><th>{{ tr('毛利率', 'Gross margin') }}</th><th>{{ tr('采购线索', 'Supplier lead') }}</th></tr></thead>
      <tbody>
        <tr v-for="product in products" :key="product.id">
          <td><div class="table-product"><span class="table-rank">{{ product.rank }}</span><div><strong>{{ product.productNameCn }}</strong><small>{{ product.category }} · {{ regionOf(product, english) }}</small><small>{{ product.keywords }}</small></div></div></td>
          <td><a class="inline-link" :href="product.sourceUrl" target="_blank" rel="noreferrer">{{ product.sourcePlatform }}<AppIcon name="external" /></a><span :class="['tag', isDemoProduct(product) ? 'warn' : 'ok']">{{ isDemoProduct(product) ? tr('演示', 'Demo') : tr('真实', 'Live') }}</span></td>
          <td><div class="heat-cell"><b>{{ product.heatScore }}</b><span><i :style="{ width: `${Math.min(100, Number(product.heatScore || 0))}%` }" /></span></div></td>
          <td><b>{{ currencyMoney(product.sourcePrice ?? product.jpPriceJpy, product.sourceCurrency || 'JPY', locale) }}</b><small>{{ currencyMoney(product.sourcePriceCny ?? product.jpPriceCny, 'CNY', locale) }}</small></td>
          <td>{{ currencyMoney(product.domesticCostCny, 'CNY', locale) }}</td>
          <td :class="Number(product.estimatedProfitCny) >= 0 ? 'positive' : 'negative'">{{ currencyMoney(product.estimatedProfitCny, 'CNY', locale) }}</td>
          <td>{{ pct(product.estimatedMargin) }}</td>
          <td><a v-if="bestLink(product)" :href="bestLink(product).url" target="_blank" rel="noreferrer">{{ bestLink(product).platform }}<small>{{ currencyMoney(bestLink(product).priceCny, 'CNY', locale) }}</small></a><span v-else>-</span></td>
        </tr>
      </tbody>
    </table>
  </section>
</template>

<script setup>
import { computed } from 'vue';
import AppIcon from './AppIcon.vue';
import { bestLink, currencyMoney, isDemoProduct, pct, regionOf } from '../lib.js';
const props = defineProps({
  products: { type: Array, default: () => [] },
  english: { type: Boolean, default: false },
});
const locale = computed(() => props.english ? 'en-US' : 'zh-CN');
function tr(chinese, english) { return props.english ? english : chinese; }
</script>
