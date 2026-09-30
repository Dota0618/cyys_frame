import { test, expect, type Page, type Locator, type Route } from '@playwright/test';
import { login, chooseScope, direct } from './browser-support';

const CONFIG = '/api/admin/platform/configurations';
const DICT = '/api/admin/platform/dictionaries';
const prefix = 'g4-browser-global-';
const errors = new WeakMap<Page, string[]>();
const owned = new WeakMap<Page, string[]>();
const pendingResponses = new WeakMap<Page, Set<() => void>>();
test.beforeEach(async ({ page, context }) => {
  const found: string[] = [];
  errors.set(page, found);
  owned.set(page, []);
  pendingResponses.set(page, new Set());
  const observe = (target: Page) => {
    target.on('pageerror', error => found.push(error.message));
    target.on('console', item => {
      if (item.type() === 'error' && /\[ErrorBoundary\]|same key/.test(item.text())) found.push(item.text());
    });
  };
  observe(page); context.on('page', observe);
});
test.afterEach(async ({ page }) => {
  pendingResponses.get(page)?.forEach(release => release());
  await page.unrouteAll({ behavior: 'wait' });
  // Keep later scenarios independent even after an assertion fails. Java removes
  // only this suite's soft-deleted rows and fixtures after the browser exits.
  const session = await page.evaluate(() => JSON.parse(sessionStorage.getItem('cyys.session') || '{}'));
  for (const path of [...(owned.get(page) || [])].reverse()) {
    const headers = { Authorization: `Bearer ${session.token}` };
    const response = await page.request.get(path, { headers });
    if (response.status() === 404) continue;
    expect(response.status()).toBe(200);
    const current = (await response.json()).data;
    expect((await page.request.delete(`${path}?version=${current.version}`, { headers })).status()).toBe(200);
  }
  expect(errors.get(page)).toEqual([]);
});

async function api(page: Page, method: string, path: string, body?: unknown) {
  const response = await direct(page, method, path, body);
  expect(response.status, JSON.stringify(response.body)).toBe(200);
  if (method === 'POST' && (path === CONFIG || path === DICT || path.endsWith('/items'))) {
    owned.get(page)?.push(`${path}/${response.body.data.id}`);
  }
  return response.body.data;
}
async function navigate(page: Page, name: string) {
  await page.getByRole('menuitem').filter({ hasText: name }).click();
  await expect(page.getByRole('button', { name: name === '通用配置' ? /新增配置$/ : /新增字典$/ })).toBeVisible();
}
const row = (page: Page, id: string) => page.locator(`tr[data-row-key="${id}"]`);
const submit = (dialog: Locator) => dialog.getByRole('button', { name: '确 定' }).click();
const cancel = (dialog: Locator) => dialog.getByRole('button', { name: '取 消', exact: true }).click();
async function remove(page: Page, target: Locator, status = 200) {
  await target.getByText('删除', { exact: true }).click();
  const response = page.waitForResponse(r => r.request().method() === 'DELETE');
  await page.getByRole('button', { name: '删 除' }).click();
  expect((await response).status()).toBe(status);
}
async function config(page: Page, suffix: string, value: string | null = 'before') {
  return api(page, 'POST', CONFIG, { name: prefix + suffix, value, type: 'SYSTEM' });
}
async function dictionary(page: Page, id: string, suffix: string) {
  return api(page, 'POST', DICT, { id, name: suffix, systemName: prefix + suffix });
}
async function item(page: Page, id: string, paramKey: string, paramValue = 'before') {
  return api(page, 'POST', `${DICT}/${id}/items`, { paramKey, paramValue });
}

test('配置真实增改删、即时校验、空字符串和文本 0/false、版本冲突保留表单', async ({ page }) => {
  await login(page, 'platform'); await navigate(page, '通用配置');
  await page.getByRole('button', { name: '新增配置' }).click();
  const create = page.getByRole('dialog', { name: '新增配置' });
  let posts = 0;
  page.on('request', req => { if (req.method() === 'POST' && req.url().endsWith(CONFIG)) posts++; });
  await create.getByLabel('配置名称', { exact: true }).fill(' ');
  await expect(create.getByText('请输入 1–50 个字符的配置名称', { exact: true })).toBeVisible();
  await submit(create); expect(posts).toBe(0);
  await create.getByLabel('配置名称', { exact: true }).fill(prefix + 'values');
  await submit(create); await expect(create).not.toBeVisible();
  const created = (await api(page, 'GET', `${CONFIG}?name=${prefix}values`)).records[0];
  owned.get(page)?.push(`${CONFIG}/${created.id}`);
  const target = row(page, created.id);
  await expect(target).toContainText('空字符串');
  for (const value of ['0', 'false', '']) {
    await target.getByText('修改', { exact: true }).click();
    const edit = page.getByRole('dialog', { name: /^修改配置/ });
    await expect(edit.getByLabel('配置名称', { exact: true })).toHaveCount(0);
    await edit.getByLabel('配置值', { exact: true }).fill(value);
    await submit(edit); await expect(edit).not.toBeVisible();
    await expect(target).toContainText(value === '' ? '空字符串' : value);
    expect((await api(page, 'GET', `${CONFIG}/${created.id}`)).value).toBe(value);
  }
  const nullRow = await config(page, 'null', null);
  await page.getByRole('img', { name: 'reload', exact: true }).click();
  await expect(row(page, nullRow.id)).toContainText('未设置');
  await target.getByText('修改', { exact: true }).click();
  const edit = page.getByRole('dialog', { name: /^修改配置/ });
  await edit.getByLabel('配置值', { exact: true }).fill('unsaved');
  const current = await api(page, 'GET', `${CONFIG}/${created.id}`);
  await api(page, 'PUT', `${CONFIG}/${created.id}`, { version: current.version, value: 'other editor', type: 'SYSTEM' });
  const conflict = page.waitForResponse(r => r.request().method() === 'PUT' && r.url().endsWith(created.id));
  await submit(edit); expect((await conflict).status()).toBe(409);
  await expect(page.getByText('配置已变更或删除，请刷新后重试', { exact: true })).toBeVisible();
  await expect(edit.getByLabel('配置值', { exact: true })).toHaveValue('unsaved');
  await cancel(edit);
  await remove(page, target, 409);
  await expect(target).toBeVisible();
  await expect(page.getByRole('button', { name: '删 除', exact: true })).toHaveCount(0);
  await page.getByRole('img', { name: 'reload', exact: true }).click();
  await expect(target).toContainText('other editor');
  await expect(page.locator('.ant-message-notice')).toHaveCount(0);
  await page.screenshot({ path: 'test-results/configurations.png', fullPage: true });
  await remove(page, target); await expect(target).toHaveCount(0);
  await remove(page, row(page, nullRow.id)); await expect(row(page, nullRow.id)).toHaveCount(0);
});

test('字典分类/项真实增改删、数字字符串编号、即时校验、重复编码和删除依赖', async ({ page }) => {
  await login(page, 'platform'); await navigate(page, '数据字典');
  await page.getByRole('button', { name: /新增字典$/ }).click();
  const create = page.getByRole('dialog', { name: '新增字典', exact: true });
  let posts = 0;
  page.on('request', req => { if (req.method() === 'POST' && req.url().includes(DICT)) posts++; });
  await create.getByLabel('字典编号', { exact: true }).fill('abc');
  await expect(create.getByText('字典编号应为 1–32 位数字')).toBeVisible();
  await submit(create); expect(posts).toBe(0);
  const id = '00990042000001';
  await create.getByLabel('字典编号', { exact: true }).fill(id);
  await create.getByLabel('内部名称', { exact: true }).fill(prefix + 'crud');
  await create.getByLabel('字典名称', { exact: true }).fill('浏览器字典');
  await submit(create); await expect(create).not.toBeVisible();
  const category = row(page, id);
  owned.get(page)?.push(`${DICT}/${id}`);
  await expect(category).toContainText(id);
  await category.getByText('字典项', { exact: true }).click();
  await page.getByRole('button', { name: /新增字典项$/ }).click();
  const addItem = page.getByRole('dialog', { name: '新增字典项', exact: true });
  await addItem.getByLabel('编码', { exact: true }).fill('0');
  await addItem.getByLabel('字典值', { exact: true }).fill(' ');
  await expect(addItem.getByText('请输入 1–20 个字符的字典值')).toBeVisible();
  await submit(addItem); expect(posts).toBe(1);
  await addItem.getByLabel('字典值', { exact: true }).fill('false');
  await submit(addItem); await expect(addItem).not.toBeVisible();
  const record = (await api(page, 'GET', `${DICT}/${id}/items`)).records[0];
  owned.get(page)?.push(`${DICT}/${id}/items/${record.id}`);
  const target = row(page, record.id);
  await expect(target).toContainText('false');
  expect(record.paramKey).toBe('0'); expect(record.paramValue).toBe('false');
  await remove(page, category, 409);
  await expect(page.getByText('字典仍有字典项，不能删除', { exact: true })).toBeVisible();
  await expect(page.getByRole('button', { name: '删 除', exact: true })).toHaveCount(0);
  await page.getByRole('button', { name: /新增字典项$/ }).click();
  await addItem.getByLabel('编码', { exact: true }).fill('0');
  await addItem.getByLabel('字典值', { exact: true }).fill('duplicate');
  await submit(addItem);
  await expect(page.getByText('当前字典内该编码已存在（包含已删除记录）', { exact: true })).toBeVisible();
  await expect(addItem.getByLabel('字典值', { exact: true })).toHaveValue('duplicate');
  await cancel(addItem);
  await target.getByText('修改', { exact: true }).click();
  const editItem = page.getByRole('dialog', { name: /^修改字典项/ });
  await expect(editItem.getByLabel('编码', { exact: true })).toHaveCount(0);
  await editItem.getByLabel('字典值', { exact: true }).fill('0');
  await submit(editItem); await expect(editItem).not.toBeVisible();
  expect((await api(page, 'GET', `${DICT}/${id}/items/${record.id}`)).paramValue).toBe('0');
  await target.getByText('修改', { exact: true }).click();
  await editItem.getByLabel('字典值', { exact: true }).fill('unsaved');
  await api(page, 'PUT', `${DICT}/${id}/items/${record.id}`, { version: 1, paramValue: 'concurrent' });
  await submit(editItem);
  await expect(page.getByText('字典记录已变更或删除，请刷新后重试', { exact: true })).toBeVisible();
  await expect(editItem.getByLabel('字典值', { exact: true })).toHaveValue('unsaved');
  await cancel(editItem);
  await category.getByText('修改', { exact: true }).click();
  const edit = page.getByRole('dialog', { name: /^修改字典：/ });
  await expect(edit.getByLabel('字典编号', { exact: true })).toHaveCount(0);
  await expect(edit.getByLabel('内部名称', { exact: true })).toHaveCount(0);
  await edit.getByLabel('字典名称', { exact: true }).fill('字典已修改');
  await submit(edit); await expect(edit).not.toBeVisible();
  await expect(category).toContainText('字典已修改');
  await expect(page.getByText(`字典已修改（${id}）· 字典项`, { exact: true })).toBeVisible();
  await page.getByRole('img', { name: 'reload', exact: true }).last().click();
  await expect(target).toContainText('concurrent');
  await expect(page.locator('.ant-message-notice')).toHaveCount(0);
  await page.screenshot({ path: 'test-results/dictionaries.png', fullPage: true });
  await remove(page, target); await expect(target).toHaveCount(0);
  await remove(page, category); await expect(category).toHaveCount(0);
  await expect(page.getByRole('button', { name: /新增字典项$/ })).toHaveCount(0);
});

test('配置、字典分类与项分页和空查询均由真实数据库返回', async ({ page }) => {
  await login(page, 'platform');
  const initialDictionaryTotal = (await direct(page, 'GET', `${DICT}?page=1&pageSize=20`)).body.data.total;
  const configurations: any[] = [], dictionaries: any[] = [], items: any[] = [];
  for (let index = 0; index < 21; index++) {
    configurations.push(await config(page, `page-${index}`, String(index)));
    dictionaries.push(await dictionary(page, String(990042100000 + index), `page-${index}`));
    items.push(await item(page, dictionaries[0].id, String(index), `page-${index}`));
  }
  async function verifyPagination(table: Locator, endpoint: string, searchLabel: string, expectedTotal = 21) {
    await expect(table.locator('tbody tr[data-row-key]')).toHaveCount(20);
    const response = page.waitForResponse(r => r.url().includes(endpoint + '?') && new URL(r.url()).searchParams.get('page') === '2');
    await table.locator('.ant-pagination-item-2').click();
    const result = (await (await response).json()).data;
    expect(result.total).toBe(expectedTotal); expect(result.records).toHaveLength(expectedTotal - 20);
    await expect(table.locator('tbody tr[data-row-key]')).toHaveCount(expectedTotal - 20);
    await expect(table.locator(`tr[data-row-key="${result.records[0].id}"]`)).toBeVisible();
    await table.locator('.ant-pagination-options-size-changer').click();
    await page.locator('.ant-select-dropdown:visible').getByText('50 条/页', { exact: true }).click();
    await expect(table.locator('tbody tr[data-row-key]')).toHaveCount(expectedTotal);
    await table.getByLabel(searchLabel, { exact: true }).fill('no-such-record');
    await table.getByRole('button', { name: '查 询' }).click();
    await expect(table.locator('.ant-empty-description')).toHaveText('暂无数据');
    await table.getByRole('button', { name: '重 置' }).click();
    await expect(table.locator('tbody tr[data-row-key]')).toHaveCount(expectedTotal);
  }
  await navigate(page, '通用配置');
  await verifyPagination(page.locator('.ant-pro-table').first(), CONFIG, '配置名称');
  await navigate(page, '数据字典');
  await verifyPagination(page.locator('.ant-pro-table').first(), DICT, '字典名称', initialDictionaryTotal + dictionaries.length);
  await row(page, dictionaries[0].id).getByText('字典项', { exact: true }).click();
  await verifyPagination(page.locator('.ant-pro-table').last(), `${DICT}/${dictionaries[0].id}/items`, '字典值');
  for (const record of items) await api(page, 'DELETE', `${DICT}/${dictionaries[0].id}/items/${record.id}?version=0`);
  for (const record of dictionaries) await api(page, 'DELETE', `${DICT}/${record.id}?version=0`);
  for (const record of configurations) await api(page, 'DELETE', `${CONFIG}/${record.id}?version=0`);
});

for (const kind of ['configuration', 'dictionary', 'item'] as const) {
  test(`${kind} 切换单位前提示，身份轮询不误清，确认后表单清空且全局数据仍可读取`, async ({ page }) => {
    await login(page, 'platform'); await chooseScope(page, '单位 A');
    const isConfig = kind === 'configuration';
    const id = kind === 'item' ? '990042200001' : '990042200002';
    const record = isConfig ? await config(page, 'switch') : await dictionary(page, id, `switch-${kind}`);
    await navigate(page, isConfig ? '通用配置' : '数据字典');
    if (kind === 'item') await row(page, id).getByText('字典项', { exact: true }).click();
    const title = isConfig ? '新增配置' : kind === 'item' ? '新增字典项' : '新增字典';
    const field = isConfig ? '配置名称' : kind === 'item' ? '字典值' : '字典名称';
    await page.getByRole('button', { name: new RegExp(title + '$') }).click();
    const dialog = page.getByRole('dialog', { name: title, exact: true });
    await dialog.getByLabel(field, { exact: true }).fill('未保存内容');
    await Promise.all([page.waitForResponse(r => r.url().endsWith('/api/auth/me')), page.evaluate(() => window.dispatchEvent(new Event('focus')))]);
    await expect(dialog.getByLabel(field, { exact: true })).toHaveValue('未保存内容');
    await chooseScope(page, '单位 B', dialog);
    await page.getByRole('button', { name: '继续编辑' }).click();
    await expect(dialog.getByLabel(field, { exact: true })).toHaveValue('未保存内容');
    await chooseScope(page, '单位 B', dialog);
    await page.getByRole('button', { name: '切换并清除' }).click();
    await expect(dialog).not.toBeVisible();
    await expect(row(page, record.id)).toBeVisible();
    if (kind === 'item') {
      await expect(page.getByRole('button', { name: new RegExp(title + '$') })).toHaveCount(0);
      await row(page, id).getByText('字典项', { exact: true }).click();
    }
    await page.getByRole('button', { name: new RegExp(title + '$') }).click();
    await expect(dialog.getByLabel(field, { exact: true })).toHaveValue('');
    await cancel(dialog);
    await api(page, 'DELETE', `${isConfig ? CONFIG : DICT}/${record.id}?version=0`);
  });
}

// Hold the real upstream response. No fabricated bodies or statuses.
async function hold(page: Page, pattern: string, method: string) {
  let release!: () => void, captured!: (status: number) => void;
  const barrier = new Promise<void>(resolve => { release = resolve; });
  pendingResponses.get(page)?.add(release);
  const ready = new Promise<number>(resolve => { captured = resolve; });
  const handler = async (route: Route) => {
    if (route.request().method() !== method) { await route.continue(); return; }
    const response = await route.fetch(); captured(response.status()); await barrier; await route.fulfill({ response });
  };
  await page.route(pattern, handler);
  return { ready, release: async () => { release(); await page.unrouteAll({ behavior: 'wait' }); } };
}

for (const kind of ['configuration', 'dictionary', 'item'] as const) {
  test(`${kind} 单位切换后迟到详情、保存成功及保存冲突不能污染新表单`, async ({ page }) => {
    await login(page, 'platform'); await chooseScope(page, '单位 A');
    const isConfig = kind === 'configuration';
    const category = isConfig ? undefined : await dictionary(page, kind === 'item' ? '990042300001' : '990042300002', `late-${kind}`);
    const record = isConfig ? await config(page, 'late') : kind === 'item' ? await item(page, category.id, 'late') : category;
    const endpoint = `${isConfig ? CONFIG : kind === 'item' ? `${DICT}/${category.id}/items` : DICT}/${record.id}`;
    const title = isConfig ? /^修改配置/ : kind === 'item' ? /^修改字典项/ : /^修改字典：/;
    const field = isConfig ? '配置值' : kind === 'item' ? '字典值' : '字典名称';
    const body = (version: number, value: string) => isConfig ? { version, value, type: 'SYSTEM' } : kind === 'item' ? { version, paramValue: value } : { version, name: value };
    await navigate(page, isConfig ? '通用配置' : '数据字典');
    const selectItems = async () => { if (kind === 'item') await row(page, category.id).getByText('字典项', { exact: true }).click(); };
    await selectItems();
    const detail = await hold(page, `**${endpoint}`, 'GET');
    await row(page, record.id).getByText('修改', { exact: true }).click(); expect(await detail.ready).toBe(200);
    await chooseScope(page, '单位 B');
    await expect(row(page, isConfig ? record.id : category.id)).toBeVisible();
    await detail.release();
    await expect(page.getByRole('dialog', { name: title })).toHaveCount(0);
    for (const conflict of [false, true]) {
      await selectItems();
      await row(page, record.id).getByText('修改', { exact: true }).click();
      const dialog = page.getByRole('dialog', { name: title });
      await dialog.getByLabel(field, { exact: true }).fill(conflict ? 'stale pending save' : 'old pending save');
      if (conflict) {
        const latest = await api(page, 'GET', endpoint);
        await api(page, 'PUT', endpoint, body(latest.version, 'other editor'));
      }
      const pending = await hold(page, `**${endpoint}`, 'PUT');
      await submit(dialog); expect(await pending.ready).toBe(conflict ? 409 : 200);
      await chooseScope(page, conflict ? '单位 B' : '单位 A', dialog);
      await page.getByRole('button', { name: '切换并清除' }).click();
      await expect(dialog).not.toBeVisible();
      await selectItems();
      await row(page, record.id).getByText('修改', { exact: true }).click();
      await dialog.getByLabel(field, { exact: true }).fill('new unsaved form');
      await pending.release();
      await expect(dialog.getByLabel(field, { exact: true })).toHaveValue('new unsaved form');
      await expect(page.locator('.ant-message-notice-error')).toHaveCount(0);
      await cancel(dialog);
    }
    const latest = await api(page, 'GET', endpoint);
    await api(page, 'DELETE', `${endpoint}?version=${latest.version}`);
    if (kind === 'item') await api(page, 'DELETE', `${DICT}/${category.id}?version=0`);
  });
}

test('全局只读权限隐藏写操作；撤销平台授权清除配置/字典表单并拒绝直接调用', async ({ page, context }) => {
  await login(page, 'platform');
  const menus = await api(page, 'GET', '/api/admin/platform/roles/create-options');
  const menuIds = menus.menus.filter((menu: any) => ['platform:scope:access', 'configuration:read', 'dictionary:read'].includes(menu.code)).map((menu: any) => menu.id);
  expect(menuIds).toHaveLength(3);
  const role = await api(page, 'POST', '/api/admin/platform/roles', { name: '浏览器全局只读', status: 1, sort: 0, menuIds });
  await api(page, 'PUT', '/api/admin/platform/roles/accounts/u-bob/roles', { roleIds: [role.id] });
  const configuration = await config(page, 'auth');
  const category = await dictionary(page, '990042400001', 'auth');
  await item(page, category.id, 'auth');
  const other = await context.newPage(); await login(other, 'bob');
  for (const name of ['通用配置', '数据字典']) {
    await other.getByRole('menuitem').filter({ hasText: name }).click();
    await expect(row(other, name === '通用配置' ? configuration.id : category.id)).toBeVisible();
    if (name === '数据字典') await row(other, category.id).getByText('字典项', { exact: true }).click();
    await expect(other.getByRole('button', { name: /新增/ })).toHaveCount(0);
    await expect(other.getByText('修改', { exact: true })).toHaveCount(0);
    await expect(other.getByText('删除', { exact: true })).toHaveCount(0);
  }
  expect((await direct(other, 'POST', CONFIG, { name: prefix + 'forbidden' })).status).toBe(403);
  expect((await direct(other, 'POST', `${DICT}/${category.id}/items`, { paramKey: 'forbidden', paramValue: 'x' })).status).toBe(403);
  const allIds = menus.menus.filter((menu: any) => menu.code === 'platform:scope:access' || /^(configuration|dictionary):/.test(menu.code)).map((menu: any) => menu.id);
  await api(page, 'PUT', '/api/admin/platform/roles', { id: role.id, version: 0, name: '浏览器全局管理', status: 1, sort: 0, menuIds: allIds });
  await other.evaluate(() => window.dispatchEvent(new Event('focus')));
  await expect(other.getByRole('button', { name: /新增字典$/ })).toBeVisible();
  const configTab = await context.newPage(); await login(configTab, 'bob'); await navigate(configTab, '通用配置');
  await configTab.getByRole('button', { name: '新增配置' }).click();
  await configTab.getByRole('dialog').getByLabel('配置名称', { exact: true }).fill('unsaved config');
  const itemTab = await context.newPage(); await login(itemTab, 'bob'); await navigate(itemTab, '数据字典');
  await row(itemTab, category.id).getByText('字典项', { exact: true }).click();
  await itemTab.getByRole('button', { name: /新增字典项$/ }).click();
  await itemTab.getByRole('dialog').getByLabel('字典值', { exact: true }).fill('unsaved item');
  await other.getByRole('button', { name: /新增字典$/ }).click();
  await other.getByRole('dialog').getByLabel('字典名称', { exact: true }).fill('unsaved dictionary');
  await api(page, 'PUT', '/api/admin/platform/roles/accounts/u-bob/roles', { roleIds: [] });
  for (const target of [other, configTab, itemTab]) {
    await target.evaluate(() => window.dispatchEvent(new Event('focus')));
    await expect(target).toHaveURL(/workspace/);
    await expect(target.getByRole('dialog')).toHaveCount(0);
    expect((await direct(target, 'GET', CONFIG)).status).toBe(403);
    expect((await direct(target, 'GET', DICT)).status).toBe(403);
  }
  await other.close(); await configTab.close(); await itemTab.close();
});
