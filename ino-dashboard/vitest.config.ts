import { defineConfig } from 'vitest/config';

// Standalone vitest config. Kept separate from vite.config.ts because Vitest
// bundles its own pinned Vite version that diverges in type from the one our
// SvelteKit plugin uses — combining them in one file produces spurious
// Plugin-type conflicts.
//
// We intentionally do NOT load the SvelteKit plugin here. Unit tests in
// `tests/unit/` cover plain TS modules (the API client, SSE parser, etc.)
// and don't need Svelte component compilation. Component tests will get
// their own config when added (probably via `@testing-library/svelte`).
export default defineConfig({
    test: {
        environment: 'jsdom',
        include: ['tests/unit/**/*.{test,spec}.{js,ts}'],
        globals: true,
    },
});
