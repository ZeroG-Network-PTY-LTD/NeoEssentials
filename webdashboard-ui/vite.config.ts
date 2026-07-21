import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// Base "" (relative) — this SPA is served from the mod's own embedded HTTP server at "/",
// but the exact host:port is whatever the admin configured (webDashboard.port), so asset URLs
// must not assume a fixed origin.
export default defineConfig({
  plugins: [react()],
  base: '',
  build: {
    outDir: 'dist',
    emptyOutDir: true,
  },
  server: {
    proxy: {
      // Dev-mode only: `npm run dev` serves the SPA on Vite's own port, but /api/* calls
      // still need to reach the mod's real embedded server — proxy them through instead of
      // fighting CORS. Adjust the target port to match webDashboard.port in your dev config.
      '/api': 'http://127.0.0.1:8642',
    },
  },
});
