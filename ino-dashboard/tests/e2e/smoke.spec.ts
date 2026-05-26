import { expect, test } from '@playwright/test';

test('home page renders the ino brand and loads without errors', async ({ page }) => {
    const consoleErrors: string[] = [];
    page.on('pageerror', (e) => consoleErrors.push(e.message));

    await page.goto('/');
    await expect(page.getByText('ino', { exact: true }).first()).toBeVisible();
    await expect(page.getByText('/// agent runtime')).toBeVisible();

    // Either the agents card grid or the error card (ino-core down) should render.
    const hasAgents = await page.getByRole('button', { name: /new session/i }).first().isVisible().catch(() => false);
    const hasError = await page.getByText('error', { exact: false }).first().isVisible().catch(() => false);
    const hasNoAgents = await page.getByText('no agents', { exact: false }).first().isVisible().catch(() => false);

    expect(hasAgents || hasError || hasNoAgents).toBe(true);
    expect(consoleErrors).toEqual([]);
});
