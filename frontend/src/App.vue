<template>
  <main>
    <div class="page-tabs">
      <a :class="['nav-button', page === 'report' ? 'active' : '']" href="/">选品报表</a>
      <a :class="['nav-button', page === 'admin' ? 'active' : '']" href="/admin">后台管理</a>
    </div>
    <AdminPage v-if="page === 'admin'" />
    <ReportPage v-else />
  </main>
</template>

<script setup>
import { computed } from 'vue';
import ReportPage from './components/ReportPage.vue';
import AdminPage from './components/AdminPage.vue';

const page = computed(() => window.location.pathname.startsWith('/admin') ? 'admin' : 'report');
</script>
