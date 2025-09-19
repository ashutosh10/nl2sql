import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/chat': 'http://localhost:8080',
      '/query': 'http://localhost:8080',
      '/a2a': 'http://localhost:8080',
      '/semantic-registry': 'http://localhost:8080',
      '/events': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        ws: false
      }
    }
  }
})
