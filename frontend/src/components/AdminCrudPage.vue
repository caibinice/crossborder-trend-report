<template>
  <section class="admin-panel">
    <div class="crud-head">
      <div><h2>{{ title }}</h2><p v-if="description" class="muted">{{ description }}</p></div>
      <button @click="editing = { ...empty }">新增</button>
    </div>
    <slot name="before" />
    <AdminDataTable :rows="rows" :columns="columns" actions @edit="editing = { ...$event }" @delete="$emit('delete', $event)" />
    <slot name="extra" />
    <AdminModalForm v-if="editing" :title="`${title}表单`" :model="editing" :fields="fields" @save="save" @cancel="editing = null" />
  </section>
</template>

<script setup>
import { ref } from 'vue';
import AdminDataTable from './AdminDataTable.vue';
import AdminModalForm from './AdminModalForm.vue';

defineProps({
  title: { type: String, required: true },
  description: { type: String, default: '' },
  rows: { type: Array, default: () => [] },
  columns: { type: Array, default: () => [] },
  fields: { type: Array, default: () => [] },
  empty: { type: Object, required: true },
});
const emit = defineEmits(['save', 'delete']);
const editing = ref(null);
function save(value) { emit('save', value, () => { editing.value = null; }); }
</script>
