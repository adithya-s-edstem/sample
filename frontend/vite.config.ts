/// <reference types="vitest/config" />
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

// https://vite.dev/config/
// In dev, all `/api` requests are proxied to the Spring Boot backend so the SPA
// can use same-origin relative URLs (no CORS in the browser during dev). The
// backend target is overridable via VITE_API_PROXY_TARGET.
const apiTarget = process.env.VITE_API_PROXY_TARGET ?? 'http://localhost:8080'

export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    proxy: {
      '/api': {
        target: apiTarget,
        changeOrigin: true,
      },
    },
  },
  // Vitest runs hook/component tests in a jsdom environment with Testing Library
  // matchers preloaded from src/test/setup.ts. The API is mocked with MSW, so
  // tests never touch the real backend.
  test: {
    globals: true,
    environment: 'jsdom',
    pool: 'threads',
    setupFiles: ['./src/test/setup.ts'],
    css: false,
    coverage: {
      provider: 'v8',
      include: ['src/**/*.{ts,tsx}'],
    },
  },
})
