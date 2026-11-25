import react from '@vitejs/plugin-react'
import { defineConfig } from 'vitest/config'

// https://vite.dev/config/ und https://vitest.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    // Im Dev-Modus gehen alle API-Aufrufe an das Spring-Boot-Backend.
    // Gleiche Origin aus Sicht des Browsers: Sitzungscookie und CSRF-Cookie funktionieren ohne CORS.
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
  test: {
    environment: 'jsdom',
    setupFiles: ['./src/test/setup.ts'],
    // Playwright-Tests (e2e/) laufen nicht unter Vitest.
    include: ['src/**/*.test.{ts,tsx}'],
  },
})
