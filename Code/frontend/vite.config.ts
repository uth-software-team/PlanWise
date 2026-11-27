import path from 'node:path'
import tailwindcss from '@tailwindcss/vite'
import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

export default defineConfig({
  plugins: [react(), tailwindcss()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    },
  },
  // Dev only: same-origin calls to /api/v1/... are forwarded unchanged to the backend
  // (context path /api). VITE_API_BASE_URL may still point elsewhere.
  server: {
    proxy: {
      '/api': { target: 'http://localhost:8080' },
    },
  },
  // `vite preview` would otherwise reuse server.proxy; the built app must not be proxied.
  preview: {
    proxy: {},
  },
})
