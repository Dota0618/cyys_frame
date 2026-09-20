import { test, expect } from '@playwright/test';
import { login, members, chooseScope } from './browser-support';

test('成员导出沿用查询条件，切换单位丢弃旧下载', async ({ page }) => {
  await login(page); await members(page);
  await page.getByLabel('成员姓名', { exact: true }).fill('单位 A');
  await page.getByRole('button', { name: '查 询' }).click();
  const request = page.waitForRequest(request => request.url().endsWith('/api/admin/users/export'));
  const pending = page.waitForEvent('download');
  await page.getByRole('button', { name: '导出当前筛选' }).click();
  expect((await request).postDataJSON()).toEqual({ name: '单位 A' });
  const file = await pending;
  expect(file.suggestedFilename()).toBe('单位成员.xlsx');
  const stream = await file.createReadStream(); const chunks: Buffer[] = [];
  for await (const chunk of stream) chunks.push(chunk);
  expect(Buffer.concat(chunks).subarray(0, 2).toString()).toBe('PK');

  let release!: () => void; let captured!: () => void;
  const barrier = new Promise<void>(resolve => { release = resolve; });
  const held = new Promise<void>(resolve => { captured = resolve; });
  const downloads: string[] = [];
  page.on('download', result => downloads.push(result.suggestedFilename()));
  await page.route('**/api/admin/users/export', async route => {
    const response = await route.fetch(); captured(); await barrier; await route.fulfill({ response });
  });
  await page.getByRole('button', { name: '导出当前筛选' }).click();
  await held;
  await chooseScope(page, '单位 B');
  await expect(page.getByRole('button', { name: '导出当前筛选' })).toHaveCount(0);
  const finished = page.waitForResponse(response => response.url().endsWith('/api/admin/users/export'));
  release();
  await (await finished).finished();
  await page.unrouteAll({ behavior: 'wait' });
  await page.evaluate(() => new Promise<void>(resolve => requestAnimationFrame(() => requestAnimationFrame(() => resolve()))));
  expect(downloads).toEqual([]);
});
