<template>
  <button class="icon-button theme-toggle" type="button" :title="theme === 'dark' ? '切换浅色模式' : '切换深色模式'" :aria-label="theme === 'dark' ? '切换浅色模式' : '切换深色模式'" @click="toggleTheme">
    <AppIcon :name="theme === 'dark' ? 'sun' : 'moon'" />
  </button>
</template>

<script setup>
import AppIcon from './AppIcon.vue';
import { useTheme } from '../theme.js';

const { theme, toggleTheme } = useTheme();
</script>
