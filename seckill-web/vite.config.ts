import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  build: {
    outDir: '../src/main/resources/static',
    emptyOutDir: false,
  },
  server: {
    proxy: {
      '/goods': 'http://localhost:8080',
      '/seckill': 'http://localhost:8080',
      '/auth': 'http://localhost:8080',
    },
  },
})
