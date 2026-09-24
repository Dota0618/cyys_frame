import { test, expect } from '@playwright/test';
import { login, chooseScope, direct } from './browser-support';

const PATH = '/api/admin/caches';
async function openCaches(page: import('@playwright/test').Page) {
  await page.getByRole('menuitem').filter({ hasText: '缓存管理' }).click();
  await expect(page.getByRole('button', { name: '新增缓存', exact: true })).toBeVisible();
}

test('缓存定义分组、刷新组与当前单位审计', async ({ page }) => {
  await login(page);
  await openCaches(page);
  await expect(page.locator('tr[data-row-key]')).toHaveCount(1);
  await expect(page.locator('tr[data-row-key="cache-user-records"]')).toContainText('用户缓存');
  const refreshed = page.waitForResponse(response => response.url().endsWith('/refresh-groups/refresh'));
  await page.getByRole('button', { name: '刷新组：用户缓存', exact: true }).click();
  expect((await refreshed).status()).toBe(200);
  await expect(page.getByText('刷新组“用户缓存”已刷新', { exact: true })).toBeVisible();
  const audit = await direct(page, 'GET', '/api/admin/audits?resource=cache&operation=REFRESH');
  expect(audit.status).toBe(200);
  expect(audit.body.data.records[0]).toMatchObject({ scopeId: 'GLOBAL', userId: 'u-alice', loginName: 'alice', realName: 'alice' });
  expect((await direct(page, 'POST', `${PATH}/sessions/refresh`)).status).toBe(404);
  await page.screenshot({ path: 'test-results/caches.png', fullPage: true });
});

test('两标签页缓存权限隔离，切换后迟到刷新结果不能回填', async ({ page, context }) => {
  await login(page);
  await openCaches(page);
  const other = await context.newPage();
  await login(other);
  await chooseScope(other, '单位 B');
  await expect(other.getByRole('menuitem').filter({ hasText: '缓存管理' })).toHaveCount(0);
  expect((await direct(other, 'POST', `${PATH}/refresh-groups/refresh`, { refreshGroup: '用户缓存', scope: 'SYSTEM' })).status).toBe(403);
  await expect(page.getByRole('button', { name: '新增缓存', exact: true })).toBeVisible();

  let release!: () => void;
  let held!: () => void;
  const barrier = new Promise<void>(resolve => { release = resolve; });
  const captured = new Promise<void>(resolve => { held = resolve; });
  await page.route(`**${PATH}/refresh-groups/refresh`, async route => {
    const response = await route.fetch();
    expect(response.status()).toBe(200);
    held();
    await barrier;
    await route.fulfill({ response });
  });
  try {
    await page.getByRole('button', { name: '刷新组：用户缓存', exact: true }).click();
    await captured;
    await chooseScope(page, '单位 B', page.getByRole('main'));
    await expect(page).toHaveURL(/workspace/);
    const switcher = page.getByRole('combobox', { name: '工作单位' });
    await switcher.click();
    await switcher.press('ArrowUp');
    await switcher.press('Enter');
    await expect(page.getByRole('menuitem').filter({ hasText: '缓存管理' })).toBeVisible();
    await openCaches(page);
    release();
    await page.unrouteAll({ behavior: 'wait' });
    await expect(page.getByText('刷新组“用户缓存”已刷新', { exact: true })).toHaveCount(0);
  } finally {
    release();
    await page.unrouteAll({ behavior: 'wait' });
    await other.close();
  }
});

test('缓存管理撤权后清除页面，隐藏入口不能直接调用', async ({ page, context }) => {
  await login(page);
  const created = await direct(page, 'POST', '/api/admin/users', {
    loginName: 'g4-cache-reader', password: 'correct-password', displayName: '缓存管理验收',
  });
  expect(created.status).toBe(200);
  const id = created.body.data.id;
  expect((await direct(page, 'PUT', `/api/admin/users/${id}/roles`, ['role-a'])).status).toBe(200);
  const reader = await context.newPage();
  await login(reader, 'g4-cache-reader');
  await openCaches(reader);
  expect((await direct(page, 'PUT', `/api/admin/users/${id}/roles`, [])).status).toBe(200);
  await reader.evaluate(() => window.dispatchEvent(new Event('focus')));
  await expect(reader).toHaveURL(/workspace/);
  await expect(reader.getByRole('button', { name: '新增缓存', exact: true })).toHaveCount(0);
  expect((await direct(reader, 'GET', PATH)).status).toBe(403);
  expect((await direct(reader, 'POST', `${PATH}/refresh-groups/refresh`, { refreshGroup: '用户缓存', scope: 'SYSTEM' })).status).toBe(403);
  await reader.close();
});
