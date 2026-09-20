import { test, expect, type Page } from '@playwright/test';
import { login } from './browser-support';

const PATH = '/api/admin/files';
async function openFiles(page: Page) {
  await page.getByRole('menuitem').filter({ hasText: '文件管理' }).click();
  await expect(page.getByRole('main').getByText('文件管理', { exact: true })).toBeVisible();
}
async function downloadedBytes(page: Page, name: string) {
  const pending = page.waitForEvent('download');
  await page.getByRole('row').filter({ hasText: name }).getByRole('button', { name: '下载', exact: true }).click();
  const download = await pending;
  const stream = await download.createReadStream();
  const chunks: Buffer[] = []; for await (const chunk of stream) chunks.push(chunk);
  expect(download.suggestedFilename()).toBe(name);
  return Buffer.concat(chunks);
}

test('私有文件上传下载、重命名和版本删除', async ({ page }) => {
  await login(page); await openFiles(page);
  const bytes = Buffer.from('私有附件内容');
  await page.locator('input[type=file]').setInputFiles({ name: 'g4-browser-file-small.txt', mimeType: 'text/plain', buffer: bytes });
  await expect(page.getByText('文件上传完成', { exact: true })).toBeVisible();
  expect(await downloadedBytes(page, 'g4-browser-file-small.txt')).toEqual(bytes);
  let row = page.getByRole('row').filter({ hasText: 'g4-browser-file-small.txt' });
  await row.getByRole('button', { name: '重命名', exact: true }).click();
  await page.getByRole('dialog').getByLabel('文件名').fill('g4-browser-file-renamed.txt');
  await page.getByRole('dialog').getByRole('button', { name: /确.*定/ }).click();
  row = page.getByRole('row').filter({ hasText: 'g4-browser-file-renamed.txt' });
  await expect(row).toBeVisible();
  await row.getByRole('button', { name: '删除', exact: true }).click();
  await page.getByRole('tooltip').getByRole('button', { name: /确.*定/ }).click();
  await expect(row).toHaveCount(0);
});

test('个人目录上传、移动、删除目录、回收恢复和永久清理', async ({ page }) => {
  await login(page); await openFiles(page);
  await page.getByRole('button', { name: '新增目录', exact: true }).click();
  await page.getByRole('dialog').getByLabel('目录名称').fill('g4-browser-directory');
  await page.getByRole('dialog').getByRole('button', { name: /确.*定/ }).click();
  await expect(page.getByRole('button', { name: '删除目录', exact: true })).toBeVisible();
  const name = 'g4-browser-directory-file.txt'; const bytes = Buffer.from('目录回收完整内容');
  await page.locator('input[type=file]').setInputFiles({ name, mimeType: 'text/plain', buffer: bytes });
  const row = page.getByRole('row').filter({ hasText: name });
  await expect(row).toBeVisible();
  await row.getByRole('checkbox').check();
  await page.getByRole('button', { name: '批量移动', exact: true }).click();
  await page.getByRole('dialog').getByRole('button', { name: /确.*定/ }).click();
  await expect(row).toHaveCount(0);
  const directory = page.getByRole('combobox', { name: '文件目录', exact: true });
  await directory.click();
  await page.locator('.ant-select-dropdown:visible .ant-select-item-option-content').getByText('默认目录', { exact: true }).click();
  await expect(page.locator('.ant-select-dropdown:visible')).toHaveCount(0);
  await expect(row).toBeVisible();
  await row.getByRole('button', { name: '移动', exact: true }).click();
  const target = page.getByRole('dialog').getByRole('combobox');
  await target.click();
  await page.locator('.ant-select-dropdown:visible .ant-select-item-option-content').getByText('g4-browser-directory', { exact: true }).click();
  await expect(page.locator('.ant-select-dropdown:visible')).toHaveCount(0);
  await page.getByRole('dialog').getByRole('button', { name: /确.*定/ }).click();
  await expect(row).toHaveCount(0);
  await directory.click();
  await page.locator('.ant-select-dropdown:visible .ant-select-item-option-content').getByText('g4-browser-directory', { exact: true }).click();
  await expect(page.locator('.ant-select-dropdown:visible')).toHaveCount(0);
  await expect(row).toBeVisible();
  await page.getByRole('button', { name: '删除目录', exact: true }).click();
  await page.getByRole('tooltip').getByRole('button', { name: /确.*定/ }).click();
  await expect(page.getByRole('button', { name: '删除目录', exact: true })).toHaveCount(0);
  expect(await downloadedBytes(page, name)).toEqual(bytes);
  await row.getByRole('checkbox').check();
  await page.getByRole('button', { name: '批量删除', exact: true }).click();
  await page.getByRole('tooltip').getByRole('button', { name: /确.*定/ }).click();
  await expect(row).toHaveCount(0);
  await page.getByRole('menuitem').filter({ hasText: '文件回收站' }).click();
  await row.getByRole('button', { name: '恢复到默认目录', exact: true }).click();
  await expect(row).toHaveCount(0);
  await openFiles(page); expect(await downloadedBytes(page, name)).toEqual(bytes);
  await row.getByRole('button', { name: '删除', exact: true }).click();
  await page.getByRole('tooltip').getByRole('button', { name: /确.*定/ }).click();
  await expect(row).toHaveCount(0);
  await page.getByRole('menuitem').filter({ hasText: '文件回收站' }).click();
  await row.getByRole('button', { name: '永久删除', exact: true }).click();
  await page.getByRole('tooltip').getByRole('button', { name: '永久删除', exact: true }).click();
  await expect(row).toHaveCount(0);
});

test('中断后刷新页面，校验原文件并只补传缺失分片', async ({ page }) => {
  await login(page); await openFiles(page);
  const bytes = Buffer.alloc(2500); for (let index = 0; index < bytes.length; index++) bytes[index] = index % 251;
  let fail = true; const counts = new Map<string, number>();
  await page.route(`**${PATH}/uploads/*/parts/*`, async route => {
    const index = route.request().url().split('/').at(-1)!;
    counts.set(index, (counts.get(index) || 0) + 1);
    if (fail && index === '1') { fail = false; await route.abort('connectionreset'); }
    else await route.continue();
  });
  await page.locator('input[type=file]').setInputFiles({ name: 'g4-browser-file-resume.bin', mimeType: 'application/octet-stream', buffer: bytes });
  await expect(page.getByRole('row').filter({ hasText: 'g4-browser-file-resume.bin' }).getByRole('button', { name: '继续上传', exact: true })).toBeVisible();
  await page.reload();
  const row = page.getByRole('row').filter({ hasText: 'g4-browser-file-resume.bin' });
  await row.getByRole('button', { name: '继续上传', exact: true }).click();
  await page.locator('input[type=file]').setInputFiles({ name: 'wrong.bin', mimeType: 'application/octet-stream', buffer: Buffer.alloc(2500) });
  await expect(page.getByText('文件内容不一致，请选择原文件', { exact: true })).toBeVisible();
  await page.locator('input[type=file]').setInputFiles({ name: 'g4-browser-file-resume.bin', mimeType: 'application/octet-stream', buffer: bytes });
  await expect(row.getByText('可用', { exact: true })).toBeVisible();
  expect(await downloadedBytes(page, 'g4-browser-file-resume.bin')).toEqual(bytes);
  expect(counts.get('0')).toBe(1); expect(counts.get('1')).toBe(2); expect(counts.get('2')).toBe(1);
  await page.screenshot({ path: 'test-results/files-resume.png', fullPage: true });
});

test('上传中切换单位清除状态，迟到响应不继续传片', async ({ page }) => {
  await login(page); await openFiles(page);
  let release!: () => void; let arrived!: () => void;
  const barrier = new Promise<void>(resolve => { release = resolve; });
  const captured = new Promise<void>(resolve => { arrived = resolve; });
  let laterParts = 0;
  await page.route(`**${PATH}/uploads/*/parts/*`, async route => {
    if (!route.request().url().endsWith('/0')) laterParts++;
    const response = await route.fetch(); arrived(); await barrier;
    await route.fulfill({ response }).catch(() => {});
  });
  try {
    await page.locator('input[type=file]').setInputFiles({ name: 'g4-browser-file-switch.bin', mimeType: 'application/octet-stream', buffer: Buffer.alloc(2200, 7) });
    await captured;
    const switcher = page.getByRole('main').getByRole('combobox', { name: '工作单位' });
    await switcher.click(); await switcher.press('ArrowDown'); await switcher.press('Enter');
    await page.getByRole('button', { name: '切换并清除' }).click();
    await expect(page.getByRole('button', { name: '上传文件', exact: true })).toHaveCount(0);
    release(); await page.unrouteAll({ behavior: 'wait' });
    await expect(page.getByRole('row').filter({ hasText: 'g4-browser-file-switch.bin' })).toHaveCount(0);
    expect(laterParts).toBe(0);
  } finally { release(); await page.unrouteAll({ behavior: 'wait' }); }
});
