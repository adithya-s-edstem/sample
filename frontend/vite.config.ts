import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

// https://vite.dev/config/
// The dev proxy for `/api` → backend is added in P0-6.
export default defineConfig({
  plugins: [react(), tailwindcss()],
})
