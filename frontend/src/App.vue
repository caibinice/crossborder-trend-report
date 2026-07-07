<template>
  <AdminPage v-if="isAdmin" />
  <ReportPage v-else />
</template>

<script setup>
import { computed } from 'vue';
import ReportPage from './components/ReportPage.vue';
import AdminPage from './components/AdminPage.vue';
const isAdmin = computed(() => window.location.pathname.startsWith('/admin'));
</script>
