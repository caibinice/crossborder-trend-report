<template>
  <button class="icon-button theme-toggle" type="button" :title="theme === 'dark' ? 'Switch to light mode' : 'Switch to dark mode'" :aria-label="theme === 'dark' ? 'Switch to light mode' : 'Switch to dark mode'" @click="toggleTheme">
    <AppIcon :name="theme === 'dark' ? 'sun' : 'moon'" />
  </button>
</template>

<script setup>
import AppIcon from './AppIcon.vue';
import { useTheme } from '../theme.js';

const { theme, toggleTheme } = useTheme();
</script>
