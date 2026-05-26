import { defineConfig } from '@playwright/test';

export default defineConfig({
    testDir: './tests/e2e',
    timeout: 30_000,
    fullyParallel: true,
    use: {
        baseURL: 'http://127.0.0.1:5173',
        trace: 'retain-on-failure',
    },
    webServer: {
        // Switch to `pnpm dev` if you prefer pnpm. npm works out of the box.
        command: 'npm run dev',
        port: 5173,
        reuseExistingServer: !process.env.CI,
        // E2E tests don't need a real ino-core unless explicitly requested.
        // For smoke tests that hit the API, start ino-core separately.
    },
});
