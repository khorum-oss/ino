import adapter from '@sveltejs/adapter-static';
import { vitePreprocess } from '@sveltejs/vite-plugin-svelte';

/** @type {import('@sveltejs/kit').Config} */
const config = {
    preprocess: vitePreprocess(),

    kit: {
        // adapter-static produces a pre-rendered SPA that Spring Boot serves
        // from src/main/resources/static/dashboard/ in production.
        adapter: adapter({
            pages: 'build',
            assets: 'build',
            fallback: 'index.html',
            precompress: false,
            strict: false,
        }),
        // Mounted under /dashboard when bundled into ino-core. Empty in dev.
        paths: {
            base: process.env.DASHBOARD_BASE ?? '',
        },
    },
};

export default config;
