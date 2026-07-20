<template>
  <section class="settings-form">
    <template v-if="section === 'sources'">
      <div class="form-grid">
        <label class="field"><span>商品数据模式</span><select v-model="draft.sourceMode"><option value="external">仅真实数据（推荐）</option><option value="mixed">真实数据 + 演示补位</option><option value="demo">仅演示数据</option></select><small class="field-help">external 模式采集失败时会明确报错，不会静默回退 Demo。</small></label>
        <label class="field switch-field"><span><b>AI 智能标准化</b><small>配置 DeepSeek Key 后翻译标题、提取中文采购词并生成有依据的选品理由</small></span><input v-model="draft.smartMode" type="checkbox" role="switch" /></label>
        <label class="field full-field"><span>国外检索数据源</span><textarea :value="joinText(draft.foreignSources)" @input="draft.foreignSources = splitText($event.target.value)" /></label>
        <label class="field full-field"><span>大分类（按行配置，按顺序取前 N 个）</span><textarea :value="joinText(draft.categories)" @input="draft.categories = splitText($event.target.value)" /></label>
        <label class="field"><span>采集大分类数</span><input v-model.number="draft.maxCategories" type="number" min="1" max="20" /></label>
        <label class="field"><span>每分类商品数</span><input v-model.number="draft.productsPerCategory" type="number" min="1" max="30" /></label>
        <label class="field"><span>默认排行维度</span><select v-model="draft.rankingMetric"><option value="sales_volume">销量指数</option><option value="sales_amount">销售额指数</option></select><small class="field-help">均为跨来源归一化指数，不冒充平台未公开的真实销量。</small></label>
        <label class="field"><span>本次商品上限</span><input :value="configuredProductLimit" disabled /><small class="field-help">分类数 × 每分类商品数，最高 500。</small></label>
        <label class="field full-field"><span>国内采购搜索站点</span><textarea :value="supplierText(draft.supplierSites)" @input="draft.supplierSites = parseSupplierText($event.target.value)" /><small class="field-help">每行格式：名称|完整链接模板，链接中必须包含 {keyword}，例如 1688|https://s.1688.com/selloffer/offer_search.htm?keywords={keyword}</small></label>
      </div>
    </template>
    <template v-else>
      <div class="form-grid">
        <label class="field full-field"><span>检索频率 Cron</span><input v-model.trim="draft.frequencyCron" placeholder="0 30 8 * * *" /><small class="field-help">Spring 六段 Cron，默认每天 08:30。</small></label>
        <label class="field"><span>每日报表商品数</span><input v-model.number="draft.maxProducts" type="number" min="1" max="100" /></label>
        <label class="field"><span>日元兜底汇率</span><input v-model.number="draft.jpyCnyRate" type="number" step="0.000001" min="0.000001" /></label>
        <label class="field"><span>默认物流成本（CNY）</span><input v-model.number="draft.defaultShippingCny" type="number" step="0.01" min="0" /></label>
        <label class="field switch-field"><span><b>自动同步公共汇率</b><small>优先使用 Frankfurter 当日汇率，失败才使用兜底值</small></span><input v-model="draft.autoExchangeRate" type="checkbox" role="switch" /></label>
      </div>
    </template>
    <div class="settings-actions"><button class="primary-button" @click="save"><AppIcon name="check" />保存配置</button></div>
  </section>
</template>

<script setup>
import { computed, reactive, watch } from 'vue';
import AppIcon from './AppIcon.vue';
import { joinText, splitText } from '../lib.js';

const props = defineProps({ settings: { type: Object, required: true }, section: { type: String, required: true } });
const emit = defineEmits(['save']);
const draft = reactive({});
const configuredProductLimit = computed(() => Math.min(500, Math.max(1, Number(draft.maxCategories || 1)) * Math.max(1, Number(draft.productsPerCategory || 1))));
function sync(settings) { Object.assign(draft, JSON.parse(JSON.stringify(settings || {}))); }
watch(() => props.settings, sync, { immediate: true, deep: true });
function supplierText(sites) { return (sites || []).map((site) => `${site.name}|${site.urlTemplate}`).join('\n'); }
function parseSupplierText(value) { return value.split(/\r?\n/).map((line) => line.trim()).filter(Boolean).map((line) => { const splitAt = line.indexOf('|'); return splitAt < 1 ? { name: line, urlTemplate: '' } : { name: line.slice(0, splitAt).trim(), urlTemplate: line.slice(splitAt + 1).trim() }; }); }
function save() {
  const payload = JSON.parse(JSON.stringify(draft));
  payload.maxProducts = configuredProductLimit.value;
  payload.domesticSources = (payload.supplierSites || []).map((site) => site.name);
  emit('save', payload);
}
</script>
