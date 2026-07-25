import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';

export default defineConfig({
  base: '/crossBorderTrend/',
  plugins: [vue()],
  server: {
    proxy: {
      '/crossBorderTrend/api': {
        target: 'http://127.0.0.1:8090',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/crossBorderTrend\/api/, '/api'),
      },
    },
  },
});
