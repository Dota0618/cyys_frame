import { expect, type Page, type Locator } from '@playwright/test';

export async function login(page: Page, name = 'alice') {
  await page.goto('/login');
  await page.getByLabel('登录账号', { exact: true }).fill(name);
  await page.getByLabel('密码', { exact: true }).fill('correct-password');
  await page.getByRole('button', { name: '登 录' }).click();
  await expect(page).toHaveURL(/workspace/);
}
export async function members(page: Page) {
  await page.getByRole('menuitem').filter({ hasText: '用户管理' }).click();
  await expect(page.getByText('成员列表', { exact: true })).toBeVisible();
}
export async function chooseScope(page: Page, name: string, container: Page | Locator = page) {
  await container.getByRole('combobox', { name: '工作单位' }).press('ArrowDown');
  await page.locator('.ant-select-dropdown:visible').getByText(name, { exact: true }).click();
}
export async function direct(page: Page, method: string, path: string, body?: unknown) {
  return page.evaluate(async ({ method, path, body }) => {
    const state = JSON.parse(sessionStorage.getItem('cyys.session') || '{}');
    const headers: Record<string, string> = { Authorization: `Bearer ${state.token}`, 'Content-Type': 'application/json' };
    if (state.scopeId) headers['X-Cyys-Scope-Id'] = state.scopeId;
    const response = await fetch(path, { method, headers, body: body === undefined ? undefined : JSON.stringify(body) });
    return { status: response.status, body: await response.json() };
  }, { method, path, body });
}
