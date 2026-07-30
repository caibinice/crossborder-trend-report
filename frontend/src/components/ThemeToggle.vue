<template>
  <button class="icon-button theme-toggle" type="button" :title="label" :aria-label="label" @click="toggleTheme">
    <AppIcon :name="theme === 'dark' ? 'sun' : 'moon'" />
  </button>
</template>

<script setup>
import { computed } from 'vue';
import AppIcon from './AppIcon.vue';
import { useTheme } from '../theme.js';

const props = defineProps({ english: { type: Boolean, default: false } });
const { theme, toggleTheme } = useTheme();
const label = computed(() => {
  if (props.english) return theme.value === 'dark' ? 'Switch to light mode' : 'Switch to dark mode';
  return theme.value === 'dark' ? '切换浅色模式' : '切换深色模式';
});
</script>
