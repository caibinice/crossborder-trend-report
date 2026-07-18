<template>
  <section v-if="!products.length" class="empty-state"><AppIcon name="search" /><h3>没有匹配商品</h3><p>调整关键词、成本或利润筛选后再试。</p></section>
  <section v-else class="data-table-shell product-table-shell">
    <table>
      <thead><tr><th>商品机会</th><th>来源</th><th>热度</th><th>源站售价</th><th>国内成本</th><th>预估利润</th><th>毛利率</th><th>采购线索</th></tr></thead>
      <tbody>
        <tr v-for="product in products" :key="product.id">
          <td><div class="table-product"><span class="table-rank">{{ product.rank }}</span><div><strong>{{ product.productNameCn }}</strong><small>{{ product.category }} · {{ regionOf(product) }}</small><small>{{ product.keywords }}</small></div></div></td>
          <td><a class="inline-link" :href="product.sourceUrl" target="_blank" rel="noreferrer">{{ product.sourcePlatform }}<AppIcon name="external" /></a><span :class="['tag', isDemoProduct(product) ? 'warn' : 'ok']">{{ isDemoProduct(product) ? '演示' : '真实' }}</span></td>
          <td><div class="heat-cell"><b>{{ product.heatScore }}</b><span><i :style="{ width: `${Math.min(100, Number(product.heatScore || 0))}%` }" /></span></div></td>
          <td><b>{{ currencyMoney(product.sourcePrice ?? product.jpPriceJpy, product.sourceCurrency || 'JPY') }}</b><small>{{ currencyMoney(product.sourcePriceCny ?? product.jpPriceCny, 'CNY') }}</small></td>
          <td>{{ currencyMoney(product.domesticCostCny, 'CNY') }}</td>
          <td :class="Number(product.estimatedProfitCny) >= 0 ? 'positive' : 'negative'">{{ currencyMoney(product.estimatedProfitCny, 'CNY') }}</td>
          <td>{{ pct(product.estimatedMargin) }}</td>
          <td><a v-if="bestLink(product)" :href="bestLink(product).url" target="_blank" rel="noreferrer">{{ bestLink(product).platform }}<small>{{ currencyMoney(bestLink(product).priceCny, 'CNY') }}</small></a><span v-else>-</span></td>
        </tr>
      </tbody>
    </table>
  </section>
</template>

<script setup>
import AppIcon from './AppIcon.vue';
import { bestLink, currencyMoney, isDemoProduct, pct, regionOf } from '../lib.js';
defineProps({ products: { type: Array, default: () => [] } });
</script>
