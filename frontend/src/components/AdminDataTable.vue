<template>
  <div class="admin-data-table">
    <div v-if="searchable" class="table-toolbar">
      <label class="table-search"><AppIcon name="search" /><input v-model.trim="query" :placeholder="searchPlaceholder" /></label>
      <span>共 {{ filteredRows.length }} 条</span>
    </div>
    <div class="data-table-shell admin-table">
      <table>
        <thead><tr><th v-for="column in columns" :key="column.key">{{ column.label }}</th><th v-if="actions">操作</th></tr></thead>
        <tbody>
          <tr v-if="!pagedRows.length"><td :colspan="columns.length + (actions ? 1 : 0)" class="table-empty"><AppIcon name="database" /><span>暂无数据</span></td></tr>
          <tr v-for="row in pagedRows" :key="row.id || row.menuKey || row.configKey || row.dictValue || row.reportDate">
            <td v-for="column in columns" :key="column.key"><span :class="cellClass(row[column.key])">{{ format(row[column.key], column.key) }}</span></td>
            <td v-if="actions" class="row-actions"><button class="text-button" @click="$emit('edit', row)">编辑</button><button class="text-button danger-text" @click="remove(row)">删除</button></td>
          </tr>
        </tbody>
      </table>
    </div>
    <div v-if="pageCount > 1" class="table-pagination"><button class="icon-button" :disabled="page === 1" @click="page--"><AppIcon name="chevron" class="flip-x" /></button><span>第 {{ page }} / {{ pageCount }} 页</span><button class="icon-button" :disabled="page === pageCount" @click="page++"><AppIcon name="chevron" /></button></div>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue';
import AppIcon from './AppIcon.vue';
import { formatDateTime } from '../lib.js';

const props = defineProps({ rows: { type: Array, default: () => [] }, columns: { type: Array, default: () => [] }, actions: { type: Boolean, default: false }, pageSize: { type: Number, default: 10 }, searchable: { type: Boolean, default: true }, searchPlaceholder: { type: String, default: '搜索当前列表' } });
const emit = defineEmits(['edit', 'delete']);
const query = ref('');
const page = ref(1);
const filteredRows = computed(() => {
  if (!query.value) return props.rows;
  const keyword = query.value.toLowerCase();
  return props.rows.filter((row) => props.columns.some((column) => format(row[column.key], column.key).toLowerCase().includes(keyword)));
});
const pageCount = computed(() => Math.max(1, Math.ceil(filteredRows.value.length / props.pageSize)));
const pagedRows = computed(() => filteredRows.value.slice((page.value - 1) * props.pageSize, page.value * props.pageSize));
watch([query, () => props.rows.length], () => { page.value = 1; });
watch(pageCount, (count) => { if (page.value > count) page.value = count; });
function remove(row) { if (window.confirm('确认删除这条记录吗？此操作不可撤销。')) emit('delete', row); }
function format(value, key = '') { if (Array.isArray(value)) return value.join(', '); if (typeof value === 'boolean') return value ? '启用' : '停用'; if (value === null || value === undefined) return ''; if (/At$/.test(key)) return formatDateTime(value); return String(value); }
function cellClass(value) { if (value === 'enabled' || value === true || value === 'success') return 'status-pill success'; if (value === 'disabled' || value === false || value === 'fail') return 'status-pill danger'; if (value === 'running') return 'status-pill pending'; return ''; }
</script>
