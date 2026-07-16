<template>
  <div class="admin-data-table">
    <div v-if="searchable" class="table-toolbar">
      <input v-model.trim="query" :placeholder="searchPlaceholder" />
      <span>{{ filteredRows.length }} 条</span>
    </div>
    <div class="admin-table">
      <table>
        <thead><tr><th v-for="column in columns" :key="column.key">{{ column.label }}</th><th v-if="actions">操作</th></tr></thead>
        <tbody>
          <tr v-if="!pagedRows.length"><td :colspan="columns.length + (actions ? 1 : 0)" class="table-empty">暂无数据</td></tr>
          <tr v-for="row in pagedRows" :key="row.id || row.menuKey || row.configKey || row.dictValue || row.reportDate">
            <td v-for="column in columns" :key="column.key"><span :class="cellClass(row[column.key])">{{ format(row[column.key]) }}</span></td>
            <td v-if="actions" class="row-actions"><button @click="$emit('edit', row)">编辑</button><button class="danger" @click="remove(row)">删除</button></td>
          </tr>
        </tbody>
      </table>
    </div>
    <div v-if="pageCount > 1" class="table-pagination">
      <button class="ghost" :disabled="page === 1" @click="page--">上一页</button>
      <span>{{ page }} / {{ pageCount }}</span>
      <button class="ghost" :disabled="page === pageCount" @click="page++">下一页</button>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue';

const props = defineProps({
  rows: { type: Array, default: () => [] },
  columns: { type: Array, default: () => [] },
  actions: { type: Boolean, default: false },
  pageSize: { type: Number, default: 10 },
  searchable: { type: Boolean, default: true },
  searchPlaceholder: { type: String, default: '搜索当前列表' },
});
const emit = defineEmits(['edit', 'delete']);
const query = ref('');
const page = ref(1);
const filteredRows = computed(() => {
  if (!query.value) return props.rows;
  const keyword = query.value.toLowerCase();
  return props.rows.filter((row) => props.columns.some((column) => format(row[column.key]).toLowerCase().includes(keyword)));
});
const pageCount = computed(() => Math.max(1, Math.ceil(filteredRows.value.length / props.pageSize)));
const pagedRows = computed(() => filteredRows.value.slice((page.value - 1) * props.pageSize, page.value * props.pageSize));
watch([query, () => props.rows.length], () => { page.value = 1; });
watch(pageCount, (count) => { if (page.value > count) page.value = count; });
function remove(row) { if (window.confirm('确认删除这条记录吗？')) emit('delete', row); }
function format(value) { if (Array.isArray(value)) return value.join(', '); if (typeof value === 'boolean') return value ? '是' : '否'; if (value === null || value === undefined) return ''; return String(value); }
function cellClass(value) { if (value === 'enabled' || value === true) return 'tag ok'; if (value === 'disabled' || value === false) return 'tag warn'; if (value === 'success') return 'tag ok'; if (value === 'fail') return 'tag warn'; return ''; }
</script>
