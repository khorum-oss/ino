import { sveltekit } from '@sveltejs/kit/vite';
import tailwindcss from '@tailwindcss/vite';
import { defineConfig } from 'vite';

export default defineConfig({
    plugins: [tailwindcss(), sveltekit()],
    server: {
        port: 5173,
        proxy: {
            // Forward `/api/*` to ino-core during dev. Override the backend
            // URL via `INO_API_PROXY` env var; defaults to localhost:8080.
            '/api': {
                target: process.env.INO_API_PROXY ?? 'http://127.0.0.1:8080',
                changeOrigin: true,
            },
        },
    },
});
