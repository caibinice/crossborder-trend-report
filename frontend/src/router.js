import { createRouter, createWebHistory } from 'vue-router';

export const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', name: 'report', component: () => import('./components/ReportPage.vue') },
    { path: '/admin/:pathMatch(.*)*', name: 'admin', component: () => import('./components/AdminPage.vue') },
    { path: '/:pathMatch(.*)*', redirect: '/' },
  ],
  scrollBehavior: () => ({ top: 0 }),
});
