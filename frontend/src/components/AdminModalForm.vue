<template>
  <Teleport to="body">
    <div class="modal-backdrop" role="presentation" @mousedown.self="$emit('cancel')">
      <form class="modal-dialog" role="dialog" aria-modal="true" :aria-labelledby="titleId" @submit.prevent="$emit('save', draft)">
        <header class="modal-header"><div><span class="overline">EDITOR</span><h3 :id="titleId">{{ title }}</h3></div><button type="button" class="icon-button" aria-label="关闭" @click="$emit('cancel')"><AppIcon name="x" /></button></header>
        <div class="modal-scroll">
          <div class="form-grid">
            <label v-for="field in fields" :key="field.key" :class="['field', { 'full-field': field.type === 'textarea', 'switch-field': field.type === 'checkbox' }]">
              <template v-if="field.type === 'checkbox'"><span><b>{{ field.label }}</b><small v-if="field.help">{{ field.help }}</small></span><input v-model="draft[field.key]" type="checkbox" role="switch" /></template>
              <template v-else>
                <span>{{ field.label }}</span>
                <select v-if="field.type === 'select'" v-model="draft[field.key]"><option v-for="option in field.options || []" :key="option.value ?? option" :value="option.value ?? option">{{ option.label ?? option }}</option></select>
                <textarea v-else-if="field.type === 'textarea'" v-model="draft[field.key]" :placeholder="field.placeholder || ''" />
                <input v-else-if="field.type === 'number'" v-model.number="draft[field.key]" type="number" :placeholder="field.placeholder || ''" />
                <input v-else v-model="draft[field.key]" :type="field.type === 'password' ? 'password' : field.type === 'email' ? 'email' : 'text'" :placeholder="field.placeholder || ''" :autocomplete="field.autocomplete || (field.type === 'password' ? 'new-password' : 'off')" />
                <small v-if="field.help" class="field-help">{{ field.help }}</small>
              </template>
            </label>
          </div>
          <slot :draft="draft" />
        </div>
        <footer class="modal-actions"><button type="button" class="secondary-button" @click="$emit('cancel')">取消</button><button class="primary-button"><AppIcon name="check" />保存更改</button></footer>
      </form>
    </div>
  </Teleport>
</template>

<script setup>
import { onMounted, onUnmounted, reactive, watch } from 'vue';
import AppIcon from './AppIcon.vue';

const props = defineProps({ title: String, model: { type: Object, required: true }, fields: { type: Array, default: () => [] } });
const emit = defineEmits(['save', 'cancel']);
const draft = reactive({ ...props.model });
const titleId = `modal-${Math.random().toString(36).slice(2)}`;
watch(() => props.model, (value) => Object.assign(draft, value), { deep: true });
function keydown(event) { if (event.key === 'Escape') emit('cancel'); }
onMounted(() => { document.body.classList.add('modal-open'); window.addEventListener('keydown', keydown); });
onUnmounted(() => { document.body.classList.remove('modal-open'); window.removeEventListener('keydown', keydown); });
</script>
