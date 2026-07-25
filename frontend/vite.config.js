import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';

export default defineConfig({
  base: '/crossBorderTrend/',
  plugins: [vue()],
  build: {
    sourcemap: false,
    rollupOptions: {
      output: {
        manualChunks(id) {
          if (!id.includes('node_modules')) return undefined;
          if (id.includes('vue') || id.includes('@vue')) return 'vendor-vue';
          return 'vendor-misc';
        },
      },
    },
  },
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
