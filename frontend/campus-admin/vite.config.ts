import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// https://vite.dev/config/
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    host: '0.0.0.0',
    port: 8081,
    open: false,
    proxy: {
      // 开发环境代理到后端，与 RuoYi 前端约定一致
      '/dev-api': {
        target: 'http://localhost:8088',
        ws: true,
        changeOrigin: true,
        rewrite: (p) => p.replace(/^\/dev-api/, '')
      },
      // WebSocket（聊天/公告推送，与三端保持一致）
      '/ws': {
        target: 'ws://localhost:8088',
        ws: true,
        changeOrigin: true
      }
    }
  }
})
