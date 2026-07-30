<template>
  <article class="product-card">
    <div class="product-media" :class="{ placeholder: !product.imageUrl || imageFailed }">
      <img v-if="product.imageUrl && !imageFailed" :src="product.imageUrl" :alt="product.productNameCn" loading="lazy" @error="imageFailed = true" />
      <AppIcon v-else name="package" />
      <span class="rank-badge">#{{ displayRank || product.rank }}</span>
      <span :class="['quality-badge', isDemoProduct(product) ? 'demo' : 'live']">{{ isDemoProduct(product) ? 'Demo' : 'Live catalog' }}</span>
    </div>
    <div class="product-content">
      <div class="product-meta"><span>{{ product.category }}</span><i /><span>{{ regionOf(product) }}</span><strong>Heat {{ product.heatScore }}</strong></div>
      <div class="score-chips"><span>Sales-volume proxy <b>{{ product.salesVolumeScore ?? '-' }}</b></span><span>Sales-value proxy <b>{{ product.salesAmountScore ?? '-' }}</b></span><span>AI <b>{{ product.aiScore ?? '-' }}</b></span></div>
      <h2>{{ product.productNameCn }}</h2>
      <p v-if="sourceTitle && sourceTitle !== product.productNameCn" class="source-title">{{ sourceTitle }}</p>
      <p class="product-reason">{{ product.reason }}</p>

      <div class="metric-strip">
        <div><span>Source price</span><b>{{ currencyMoney(product.sourcePrice ?? product.jpPriceJpy, product.sourceCurrency || 'JPY') }}</b></div>
        <div><span>CNY equivalent</span><b>{{ currencyMoney(product.sourcePriceCny ?? product.jpPriceCny, 'CNY') }}</b></div>
        <div><span>Estimated profit</span><b :class="Number(product.estimatedProfitCny) >= 0 ? 'positive' : 'negative'">{{ currencyMoney(product.estimatedProfitCny, 'CNY') }}</b></div>
        <div><span>Gross margin</span><b>{{ pct(product.estimatedMargin) }}</b></div>
      </div>

      <div class="product-links">
        <a class="source-link" :href="product.sourceUrl" :title="product.sourcePlatform" target="_blank" rel="noreferrer"><AppIcon name="external" /><span>{{ product.sourcePlatform }}</span></a>
        <button v-if="best" type="button" class="supplier-link" @click="supplierOpen = true"><span>View {{ product.domesticLinks?.length || 0 }} China sourcing channels</span><b>{{ money(best.priceCny, CNY) }}</b><small>Lowest estimate · {{ best.platform }}</small></button>
      </div>
    </div>
  </article>
  <Teleport to="body">
    <div v-if="supplierOpen" class="modal-backdrop" role="presentation" @mousedown.self="supplierOpen = false">
      <section class="modal-dialog supplier-dialog" role="dialog" aria-modal="true" aria-label="China sourcing search">
        <header class="modal-header"><div><span class="overline">CHINA SOURCING</span><h3>Supplier Search</h3><p>{{ product.productNameCn }}</p></div><button class="icon-button" aria-label="Close" @click="supplierOpen = false"><AppIcon name="x" /></button></header>
        <div class="modal-scroll supplier-options">
          <p class="supplier-tip">Search terms are normalized in English. Verify platform prices and sales signals on the linked source page.</p>
          <a v-for="link in product.domesticLinks || []" :key="`${link.platform}-${link.url}`" :href="link.url" target="_blank" rel="noreferrer" class="supplier-option">
            <span class="metric-icon blue"><AppIcon name="search" /></span><div><b>{{ link.platform }}</b><small>{{ link.title }}</small><p>{{ link.note }}</p></div><strong>{{ money(link.priceCny, CNY) }}</strong><AppIcon name="external" />
          </a>
        </div>
      </section>
    </div>
  </Teleport>
</template>

<script setup>
import { computed, ref } from 'vue';
import AppIcon from './AppIcon.vue';
import { CNY, bestLink, currencyMoney, isDemoProduct, money, pct, regionOf } from '../lib.js';

const props = defineProps({ product: { type: Object, required: true }, displayRank: { type: Number, default: 0 } });
const imageFailed = ref(false);
const supplierOpen = ref(false);
const sourceTitle = computed(() => props.product.sourceTitle || props.product.productNameJp || '');
const best = computed(() => bestLink(props.product));
</script>
