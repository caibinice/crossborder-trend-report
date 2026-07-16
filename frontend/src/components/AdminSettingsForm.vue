<template>
  <section class="settings-form">
    <template v-if="section === 'sources'">
      <label>国外检索数据源<textarea :value="joinText(draft.foreignSources)" @input="draft.foreignSources = splitText($event.target.value)" /></label>
      <label>国内检索数据源<textarea :value="joinText(draft.domesticSources)" @input="draft.domesticSources = splitText($event.target.value)" /></label>
    </template>
    <template v-else>
      <label>检索频率 Cron<input v-model.trim="draft.frequencyCron" placeholder="0 30 8 * * *" /></label>
      <label>每日报表商品数<input v-model.number="draft.maxProducts" type="number" min="1" max="100" /></label>
      <label>日元汇率<input v-model.number="draft.jpyCnyRate" type="number" step="0.000001" min="0.000001" /></label>
      <label>默认物流成本<input v-model.number="draft.defaultShippingCny" type="number" step="0.01" min="0" /></label>
      <label class="check"><input v-model="draft.smartMode" type="checkbox" />启用智能模式</label>
    </template>
    <button @click="save">保存配置</button>
  </section>
</template>

<script setup>
import { reactive, watch } from 'vue';
import { joinText, splitText } from '../lib.js';

const props = defineProps({ settings: { type: Object, required: true }, section: { type: String, required: true } });
const emit = defineEmits(['save']);
const draft = reactive({});

function sync(settings) {
  Object.assign(draft, JSON.parse(JSON.stringify(settings || {})));
}

watch(() => props.settings, sync, { immediate: true, deep: true });
function save() { emit('save', JSON.parse(JSON.stringify(draft))); }
</script>
