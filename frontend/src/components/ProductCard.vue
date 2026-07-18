<template>
  <article class="product-card">
    <div class="product-media" :class="{ placeholder: !product.imageUrl || imageFailed }">
      <img v-if="product.imageUrl && !imageFailed" :src="product.imageUrl" :alt="product.productNameCn" loading="lazy" @error="imageFailed = true" />
      <AppIcon v-else name="package" />
      <span class="rank-badge">#{{ product.rank }}</span>
      <span :class="['quality-badge', isDemoProduct(product) ? 'demo' : 'live']">{{ isDemoProduct(product) ? '演示' : '真实目录' }}</span>
    </div>
    <div class="product-content">
      <div class="product-meta"><span>{{ product.category }}</span><i /><span>{{ regionOf(product) }}</span><strong>热度 {{ product.heatScore }}</strong></div>
      <h2>{{ product.productNameCn }}</h2>
      <p v-if="sourceTitle && sourceTitle !== product.productNameCn" class="source-title">{{ sourceTitle }}</p>
      <p class="product-reason">{{ product.reason }}</p>

      <div class="metric-strip">
        <div><span>源站售价</span><b>{{ currencyMoney(product.sourcePrice ?? product.jpPriceJpy, product.sourceCurrency || 'JPY') }}</b></div>
        <div><span>折合人民币</span><b>{{ currencyMoney(product.sourcePriceCny ?? product.jpPriceCny, 'CNY') }}</b></div>
        <div><span>预估利润</span><b :class="Number(product.estimatedProfitCny) >= 0 ? 'positive' : 'negative'">{{ currencyMoney(product.estimatedProfitCny, 'CNY') }}</b></div>
        <div><span>毛利率</span><b>{{ pct(product.estimatedMargin) }}</b></div>
      </div>

      <div class="product-links">
        <a class="source-link" :href="product.sourceUrl" target="_blank" rel="noreferrer"><AppIcon name="external" />{{ product.sourcePlatform }}</a>
        <a v-if="best" class="supplier-link" :href="best.url" target="_blank" rel="noreferrer"><span>最低采购估算</span><b>{{ money(best.priceCny, CNY) }}</b><small>{{ best.platform }}</small></a>
      </div>
    </div>
  </article>
</template>

<script setup>
import { computed, ref } from 'vue';
import AppIcon from './AppIcon.vue';
import { CNY, bestLink, currencyMoney, isDemoProduct, money, pct, regionOf } from '../lib.js';

const props = defineProps({ product: { type: Object, required: true } });
const imageFailed = ref(false);
const sourceTitle = computed(() => props.product.sourceTitle || props.product.productNameJp || '');
const best = computed(() => bestLink(props.product));
</script>
