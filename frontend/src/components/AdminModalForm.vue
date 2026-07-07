<template>
  <div class="modal">
    <form class="modal-card" @submit.prevent="$emit('save', draft)">
      <h3>{{ title }}</h3>
      <label v-for="field in fields" :key="field.key">
        {{ field.label }}
        <select v-if="field.type==='select'" v-model="draft[field.key]"><option v-for="option in field.options || []" :key="option.value ?? option" :value="option.value ?? option">{{ option.label ?? option }}</option></select>
        <textarea v-else-if="field.type==='textarea'" v-model="draft[field.key]" />
        <input v-else-if="field.type==='checkbox'" type="checkbox" v-model="draft[field.key]" />
        <input v-else-if="field.type==='number'" type="number" v-model.number="draft[field.key]" />
        <input v-else v-model="draft[field.key]" />
      </label>
      <slot :draft="draft"></slot>
      <div class="modal-actions"><button>保存</button><button type="button" class="ghost" @click="$emit('cancel')">取消</button></div>
    </form>
  </div>
</template>
<script setup>
import { reactive, watch } from 'vue';
const props = defineProps({ title:String, model:{type:Object,required:true}, fields:{type:Array,default:()=>[]} });
defineEmits(['save','cancel']);
const draft = reactive({ ...props.model });
watch(() => props.model, (v) => Object.assign(draft, v), { deep:true });
</script>
