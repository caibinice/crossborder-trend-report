<template>
  <article class="product">
    <div class="top">
      <span class="rank">#{{ product.rank }}</span>
      <span>{{ product.category }}</span>
      <span>{{ regionOf(product) }}</span>
      <strong>热度 {{ product.heatScore }}</strong>
    </div>
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
</template>

<script setup>
import { CNY, YEN, money, pct, regionOf } from '../lib.js';
defineProps({ product: { type: Object, required: true } });
</script>
