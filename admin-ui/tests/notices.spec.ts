import { test, expect, type Page } from '@playwright/test';
import { login, chooseScope, direct } from './browser-support';

const BASE = '/api/admin/org-notices';
async function openNotices(page: Page) {
  await page.getByRole('menuitem').filter({ hasText: '单位通知' }).click();
  await expect(page.getByRole('main').getByText('单位通知', { exact: true })).toBeVisible();
}
async function editor(page: Page, title: string) {
  await page.getByRole('button', { name: '新增通知', exact: true }).click();
  const dialog = page.getByRole('dialog', { name: '新增通知', exact: true });
  await dialog.getByLabel('所属部门', { exact: true }).click();
  await page.locator('.ant-select-tree-list-holder:visible').getByText('g1-a-root', { exact: true }).click();
  await expect(page.locator('.ant-select-dropdown:visible')).toHaveCount(0);
  await dialog.getByLabel('通知标题', { exact: true }).fill(title);
  await dialog.getByLabel('通知内容', { exact: true }).fill('<script>通知正文作为文字展示</script>');
  return dialog;
}

test('真实通知上传与素材选择、排序、阅读、解除关联保留文件', async ({ page }) => {
  await login(page);
  await page.getByRole('menuitem').filter({ hasText: '文件管理' }).click();
  const name = 'g4-browser-notice-library.txt';
  await page.locator('input[type=file]').setInputFiles({ name, mimeType: 'text/plain', buffer: Buffer.from('已有素材') });
  await expect(page.getByRole('row').filter({ hasText: name })).toBeVisible();
  await openNotices(page);
  const title = 'g4-browser-notice';
  let form = await editor(page, title);
  await form.getByRole('combobox', { name: '通知类型' }).click();
  await page.locator('.ant-select-dropdown:visible').getByText('普通通知', { exact: true }).click();
  await expect(page.locator('.ant-select-dropdown:visible')).toHaveCount(0);
  const uploaded = 'g4-browser-notice-new.txt';
  await form.locator('input[type=file]').setInputFiles({ name: uploaded, mimeType: 'text/plain', buffer: Buffer.from('新增素材') });
  await expect(form.locator('li').filter({ hasText: uploaded })).toBeVisible();
  await form.getByRole('button', { name: '选择已有附件' }).click();
  const picker = page.getByRole('dialog', { name: '选择已有附件', exact: true });
  await picker.getByRole('row').filter({ hasText: name }).getByRole('checkbox').check();
  await picker.getByRole('button', { name: '确定选择' }).click();
  await expect(picker).not.toBeVisible();
  await form.locator('li').filter({ hasText: name }).getByRole('button', { name: /上\s*移/ }).click();
  await form.getByRole('button', { name: /确.*定/ }).click();
  await expect(form).not.toBeVisible();
  const row = page.getByRole('row').filter({ hasText: title });
  await expect(row).toContainText('普通通知');
  const list = await direct(page, 'GET', `${BASE}?title=${title}`);
  const id = list.body.data.records[0].id;
  let detail = (await direct(page, 'GET', `${BASE}/${id}`)).body.data;
  expect(detail.attachments.map((file: { name: string }) => file.name)).toEqual([name, uploaded]);
  const file = (await direct(page, 'GET', '/api/admin/files')).body.data.records.find((item: { name: string }) => item.name === name);
  expect((await direct(page, 'DELETE', `/api/admin/files/${file.id}?version=${file.version}`)).status).toBe(409);
  await row.getByRole('button', { name: '查看', exact: true }).click();
  const viewing = page.getByRole('dialog', { name: title, exact: true });
  await expect(viewing.getByText('<script>通知正文作为文字展示</script>', { exact: true })).toBeVisible();
  const pending = page.waitForEvent('download');
  await viewing.getByRole('button', { name, exact: true }).click();
  const download = await pending;
  expect(download.suggestedFilename()).toBe(name);
  const stream = await download.createReadStream(); const chunks: Buffer[] = [];
  for await (const chunk of stream) chunks.push(chunk);
  expect(Buffer.concat(chunks).toString()).toBe('已有素材');
  await viewing.getByRole('button', { name: 'Close', exact: true }).click();
  await expect(row).toContainText('已读');
  await row.getByRole('button', { name: '阅读记录', exact: true }).click();
  const receipts = page.getByRole('dialog', { name: '阅读记录', exact: true });
  await expect(receipts.getByRole('row').filter({ hasText: '单位 A 姓名' })).toBeVisible();
  await receipts.getByRole('button', { name: 'Close', exact: true }).click();
  await row.getByRole('button', { name: '编辑', exact: true }).click();
  form = page.getByRole('dialog', { name: '编辑通知', exact: true });
  await form.locator('li').filter({ hasText: name }).getByRole('button', { name: '移除关联' }).click();
  await form.getByRole('button', { name: /确.*定/ }).click();
  await expect(form).not.toBeVisible();
  detail = (await direct(page, 'GET', `${BASE}/${id}`)).body.data;
  expect(detail.attachments.map((item: { name: string }) => item.name)).toEqual([uploaded]);
  expect(detail.notice.readCount).toBe(1);
  expect((await direct(page, 'GET', '/api/admin/files')).body.data.records.some((item: { id: string }) => item.id === file.id)).toBe(true);
  await row.getByRole('button', { name: '删除', exact: true }).click();
  await page.getByRole('tooltip').getByRole('button', { name: /确.*定/ }).click();
  await expect(row).toHaveCount(0);
});

test('上传完成仍保护未保存表单，切换单位清除附件和编辑内容', async ({ page }) => {
  await login(page); await openNotices(page);
  const form = await editor(page, 'g4-browser-notice-unsaved');
  await form.locator('input[type=file]').setInputFiles({ name: 'g4-browser-notice-unsaved.txt', mimeType: 'text/plain', buffer: Buffer.from('未保存关联') });
  await expect(form.getByText('文件上传完成', { exact: true })).toBeVisible();
  await chooseScope(page, '单位 B', form);
  await expect(page.getByText('当前表单尚未保存，切换后将清除填写内容。', { exact: true })).toBeVisible();
  await page.getByRole('button', { name: '继续编辑', exact: true }).click();
  await expect(form.getByLabel('通知标题', { exact: true })).toHaveValue('g4-browser-notice-unsaved');
  await chooseScope(page, '单位 B', form);
  await page.getByRole('button', { name: '切换并清除', exact: true }).click();
  await expect(form).not.toBeVisible();
  await expect(page.getByRole('button', { name: '新增通知', exact: true })).toHaveCount(0);
  expect((await direct(page, 'GET', `${BASE}/create-options`)).status).toBe(403);
  await chooseScope(page, '单位 A', page.getByRole('main'));
  await page.getByRole('button', { name: '新增通知', exact: true }).click();
  await expect(form.getByLabel('通知标题', { exact: true })).toHaveValue('');
  await expect(form.getByText('g4-browser-notice-unsaved.txt', { exact: true })).toHaveCount(0);
});

test('上传期间移除附件，完成响应不恢复旧的字段值', async ({ page }) => {
  await login(page); await openNotices(page);
  const form = await editor(page, 'g4-browser-notice-upload-race');
  const first = 'g4-browser-notice-first.txt', second = 'g4-browser-notice-second.txt';
  await form.locator('input[type=file]').setInputFiles({ name: first, mimeType: 'text/plain', buffer: Buffer.from('先前附件') });
  await expect(form.locator('li').filter({ hasText: first })).toBeVisible();
  let release!: () => void, arrived!: () => void;
  const barrier = new Promise<void>(resolve => { release = resolve; });
  const captured = new Promise<void>(resolve => { arrived = resolve; });
  await page.route('**/api/admin/files', async route => {
    if (route.request().method() !== 'POST') { await route.continue(); return; }
    const response = await route.fetch(); arrived(); await barrier;
    await route.fulfill({ response });
  });
  try {
    await form.locator('input[type=file]').setInputFiles({ name: second, mimeType: 'text/plain', buffer: Buffer.from('完成较晚附件') });
    await captured;
    await form.locator('li').filter({ hasText: first }).getByRole('button', { name: '移除关联' }).click();
    release();
    await expect(form.locator('li').filter({ hasText: second })).toBeVisible();
    await expect(form.locator('li').filter({ hasText: first })).toHaveCount(0);
  } finally { release(); }
});
