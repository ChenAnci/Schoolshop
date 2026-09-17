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
    port: 8083,
    open: false,
    proxy: {
      // 开发环境代理到后端（本机后端实际运行在 8088），与 RuoYi 前端约定一致
      '/dev-api': {
        target: 'http://localhost:8088',
        changeOrigin: true,
        ws: true,
        rewrite: (p) => p.replace(/^\/dev-api/, '')
      },
      // WebSocket 聊天/公告推送代理（浏览器连 /ws，转发到后端 /ws）
      '/ws': {
        target: 'ws://localhost:8088',
        ws: true,
        changeOrigin: true
      }
    }
  }
})
