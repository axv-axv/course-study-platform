import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  css: {
    preprocessorOptions: {
      scss: {
        additionalData: `@use "@/styles/variables.scss" as *;`
      }
    }
  },
  server: {
    port: 5173,
    proxy: {
      // 开发环境代理：后端接口统一前缀 /api/v1
      '/api/v1': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true
      },
      // 文件静态访问（上传后的文件 url 形如 /files/{fileId} 或 /api/v1/files/...）
      '/files': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true
      }
    }
  },
  build: {
    rollupOptions: {
      output: {
        manualChunks: {
          'vue-vendor': ['vue', 'vue-router', 'pinia'],
          'element-plus': ['element-plus', '@element-plus/icons-vue'],
          axios: ['axios']
        }
      }
    },
    chunkSizeWarningLimit: 1200
  }
})
