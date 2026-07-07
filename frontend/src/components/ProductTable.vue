<template>
  <section v-if="!products.length" class="empty">没有符合筛选条件的商品。</section>
  <section v-else class="table-wrap">
    <table>
      <thead>
        <tr><th>排名</th><th>商品</th><th>品类</th><th>区域</th><th>热度</th><th>日本售价</th><th>国内成本</th><th>利润</th><th>毛利率</th><th>最优供货</th><th>来源</th></tr>
      </thead>
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
</template>

<script setup>
import { CNY, YEN, bestLink, money, pct, regionOf } from '../lib.js';
defineProps({ products: { type: Array, default: () => [] } });
</script>
