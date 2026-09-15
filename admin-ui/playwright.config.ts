import { defineConfig } from '@playwright/test';

if (!process.env.CYYS_API_TARGET) throw new Error('Run through BrowserAcceptance with owned MySQL fixtures.');
export default defineConfig({
  testDir: './tests', workers: 1, retries: 0, timeout: 35_000,
  reporter: [['list']],
  use: { baseURL: 'http://127.0.0.1:18041', channel: 'chrome', headless: true,
    actionTimeout: 8_000,
    viewport: { width: 1440, height: 1000 }, screenshot: 'only-on-failure', trace: 'off' },
  webServer: { command: 'node tests/server.mjs', url: 'http://127.0.0.1:18041/login',
    reuseExistingServer: false, timeout: 20_000 },
});
