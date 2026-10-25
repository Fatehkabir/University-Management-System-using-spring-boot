import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// In development the API is proxied to the Spring Boot backend, so the browser
// only ever talks to one origin (same as production behind nginx).
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': process.env.VITE_API_TARGET || 'http://localhost:8080',
    },
  },
});
