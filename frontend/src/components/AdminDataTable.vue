<template>
  <div class="admin-table">
    <table>
      <thead><tr><th v-for="c in columns" :key="c.key">{{ c.label }}</th><th v-if="actions">操作</th></tr></thead>
      <tbody>
        <tr v-for="row in rows" :key="row.id || row.menuKey || row.configKey || row.dictValue || row.reportDate">
          <td v-for="c in columns" :key="c.key"><span :class="cellClass(row[c.key])">{{ format(row[c.key]) }}</span></td>
          <td v-if="actions" class="row-actions"><button @click="$emit('edit', row)">编辑</button><button class="danger" @click="$emit('delete', row)">删除</button></td>
        </tr>
      </tbody>
    </table>
  </div>
</template>
<script setup>
defineProps({ rows:{type:Array,default:()=>[]}, columns:{type:Array,default:()=>[]}, actions:{type:Boolean,default:false} });
defineEmits(['edit','delete']);
function format(v){ if(Array.isArray(v)) return v.join(', '); if(typeof v==='boolean') return v?'是':'否'; if(v===null||v===undefined) return ''; return String(v); }
function cellClass(v){ if(v==='enabled'||v===true) return 'tag ok'; if(v==='disabled'||v===false) return 'tag warn'; if(v==='success') return 'tag ok'; if(v==='fail') return 'tag warn'; return ''; }
</script>
