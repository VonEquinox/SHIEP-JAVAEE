import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    proxy: {
      // 前端所有 /api 请求转发到单体 Spring Boot 后端
      '/api': 'http://127.0.0.1:8080',
    },
  },
});
