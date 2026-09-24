import { test, expect, type Page } from '@playwright/test';
import { login, chooseScope, direct } from './browser-support';

const errors = new WeakMap<Page, string[]>();
const releases = new WeakMap<Page, Set<() => void>>();

test.beforeEach(async ({ page, context }) => {
  const found: string[] = [];
  errors.set(page, found);
  releases.set(page, new Set());
  const observe = (target: Page) => target.on('pageerror', error => found.push(error.message));
  observe(page);
  context.on('page', observe);
});
test.afterEach(async ({ page }) => {
  releases.get(page)?.forEach(release => release());
  await page.unrouteAll({ behavior: 'wait' });
  expect(errors.get(page)).toEqual([]);
});

async function navigate(page: Page, title: string) {
  await page.getByRole('menuitem').filter({ hasText: title }).click();
  await expect(page.getByRole('main').getByText(title, { exact: true })).toBeVisible();
}

for (const entry of [
  { title: '操作审计', path: '/api/admin/audits', route: '/system/audits', key: 'operation' },
  { title: '登录记录', path: '/api/admin/login-audits', route: '/system/login-audits', key: 'login' },
]) {
  test(`${entry.title}：真实记录、过滤与只读页面`, async ({ page }) => {
    await login(page);
    if (entry.key === 'operation') {
      const created = await direct(page, 'POST', '/api/admin/departments', {
        name: 'g4-browser-audit-source', code: 'g4-browser-audit-source',
      });
      expect(created.status).toBe(200);
    } else {
      expect((await page.request.post('/api/auth/login', {
        data: { loginName: 'alice', password: 'wrong', scopeId: 'scope-a' },
      })).status()).toBe(401);
    }
    await navigate(page, entry.title);
    await expect(page.locator('tr[data-row-key]').first()).toBeVisible();
    expect((await direct(page, 'GET', entry.path)).body.data.records.every(
      (record: { scopeId: string }) => record.scopeId === 'scope-a',
    )).toBe(true);
    if (entry.key === 'login') {
      await expect(page.getByRole('columnheader', { name: '登录名', exact: true })).toBeVisible();
      await expect(page.getByRole('columnheader', { name: '真实姓名', exact: true })).toBeVisible();
      await expect(page.getByRole('columnheader', { name: '账号', exact: true })).toHaveCount(0);
      await page.getByLabel('登录名', { exact: true }).fill('no-such-audit-record');
      await page.getByRole('button', { name: '查 询' }).click();
      await expect(page.locator('tr[data-row-key]')).toHaveCount(0);
      await page.getByRole('button', { name: '重 置' }).click();
      await expect(page.locator('tr[data-row-key]').first()).toBeVisible();
    } else {
      await expect(page.getByRole('columnheader', { name: '登录名', exact: true })).toBeVisible();
      await expect(page.getByRole('columnheader', { name: '真实姓名', exact: true })).toBeVisible();
      await page.getByLabel('操作', { exact: true }).click();
      await page.locator('.ant-select-dropdown:visible .ant-select-item-option-content').filter({ hasText: /^新增$/ }).click();
      await page.getByRole('button', { name: '查 询' }).click();
      await expect(page.locator('tr[data-row-key]').first()).toContainText('新增');
    }
    await expect(page.getByRole('button', { name: /新增|删除|修改/ })).toHaveCount(0);
    await page.screenshot({ path: `test-results/${entry.key}-audits.png`, fullPage: true });
  });

  test(`${entry.title}：两标签页单位独立，切换清理过滤及迟到结果`, async ({ page, context }) => {
    await login(page);
    if (entry.key === 'operation') {
      expect((await direct(page, 'POST', '/api/admin/departments', {
        name: 'g4-browser-audit-switch', code: 'g4-browser-audit-switch',
      })).status).toBe(200);
    }
    await navigate(page, entry.title);
    const initial = await direct(page, 'GET', entry.path);
    expect(initial.status).toBe(200);
    expect(initial.body.data.records.length).toBeGreaterThan(0);
    const originalId = initial.body.data.records[0].id;
    const other = await context.newPage();
    await login(other);
    await navigate(other, entry.title);
    await chooseScope(other, '单位 B', other.getByRole('main'));
    await expect(other.locator(`tr[data-row-key="${originalId}"]`)).toHaveCount(0);
    await expect(page.locator(`tr[data-row-key="${originalId}"]`)).toBeVisible();

    let release!: () => void;
    let held!: () => void;
    const barrier = new Promise<void>(resolve => { release = resolve; });
    const captured = new Promise<void>(resolve => { held = resolve; });
    releases.get(page)?.add(release);
    await page.route(`**${entry.path}?*`, async route => {
      if (route.request().headers()['x-cyys-scope-id'] === 'scope-a') {
        const response = await route.fetch();
        held();
        await barrier;
        await route.fulfill({ response });
      } else await route.continue();
    });
    if (entry.key === 'login') await page.getByLabel('登录名', { exact: true }).fill('alice');
    await page.getByRole('button', { name: '查 询' }).click();
    await captured;
    const newScopeResponse = page.waitForResponse(response => response.url().includes(entry.path)
      && response.request().headers()['x-cyys-scope-id'] === 'scope-b');
    await chooseScope(page, '单位 B', page.getByRole('main'));
    expect((await newScopeResponse).status()).toBe(200);
    release();
    await page.unrouteAll({ behavior: 'wait' });
    await expect(page.locator(`tr[data-row-key="${originalId}"]`)).toHaveCount(0);
    if (entry.key === 'login') await expect(page.getByLabel('登录名', { exact: true })).toHaveValue('');
    const result = await direct(page, 'GET', entry.path);
    expect(result.status).toBe(200);
    expect(result.body.data.records.every((record: { scopeId: string }) => record.scopeId === 'scope-b')).toBe(true);
    await other.close();
  });

  test(`${entry.title}：撤权清除页面和记录，直接访问仍拒绝`, async ({ page, context }) => {
    await login(page);
    const created = await direct(page, 'POST', '/api/admin/users', {
      loginName: `g4-audit-reader-${entry.key}`, password: 'correct-password', displayName: '审计只读验收',
    });
    expect(created.status).toBe(200);
    const id = created.body.data.id;
    expect((await direct(page, 'PUT', `/api/admin/users/${id}/roles`, ['role-a'])).status).toBe(200);
    const reader = await context.newPage();
    await login(reader, `g4-audit-reader-${entry.key}`);
    await navigate(reader, entry.title);
    await expect(reader.locator('tr[data-row-key]').first()).toBeVisible();
    expect((await direct(page, 'PUT', `/api/admin/users/${id}/roles`, [])).status).toBe(200);
    await reader.evaluate(() => window.dispatchEvent(new Event('focus')));
    await expect(reader.getByRole('main').getByText(entry.title, { exact: true })).not.toBeVisible();
    await expect(reader.locator('tr[data-row-key]')).toHaveCount(0);
    expect((await direct(reader, 'GET', entry.path)).status).toBe(403);
    await reader.goto(entry.route);
    await expect(reader.getByRole('main').getByText(entry.title, { exact: true })).not.toBeVisible();
    await reader.close();
  });
}
