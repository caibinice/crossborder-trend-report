<template>
  <section class="settings-form">
    <template v-if="section === 'sources'">
      <div class="form-grid">
        <label class="field"><span>商品数据模式</span><select v-model="draft.sourceMode"><option value="external">仅真实数据（推荐）</option><option value="mixed">真实数据 + 演示补位</option><option value="demo">仅演示数据</option></select><small class="field-help">external 模式采集失败时会明确报错，不会静默回退 Demo。</small></label>
        <label class="field switch-field"><span><b>AI 智能标准化</b><small>配置 DeepSeek Key 后翻译标题、归类并生成有依据的选品理由</small></span><input v-model="draft.smartMode" type="checkbox" role="switch" /></label>
        <label class="field full-field"><span>国外检索数据源</span><textarea :value="joinText(draft.foreignSources)" @input="draft.foreignSources = splitText($event.target.value)" /></label>
        <label class="field full-field"><span>国内采购来源</span><textarea :value="joinText(draft.domesticSources)" @input="draft.domesticSources = splitText($event.target.value)" /></label>
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
import { reactive, watch } from 'vue';
import AppIcon from './AppIcon.vue';
import { joinText, splitText } from '../lib.js';

const props = defineProps({ settings: { type: Object, required: true }, section: { type: String, required: true } });
const emit = defineEmits(['save']);
const draft = reactive({});
function sync(settings) { Object.assign(draft, JSON.parse(JSON.stringify(settings || {}))); }
watch(() => props.settings, sync, { immediate: true, deep: true });
function save() { emit('save', JSON.parse(JSON.stringify(draft))); }
</script>
