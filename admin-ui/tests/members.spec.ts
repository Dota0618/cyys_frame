import { test, expect, type Page, type Locator } from '@playwright/test';

const browserErrors = new WeakMap<Page, string[]>();
test.beforeEach(async ({ page, context }) => {
  const errors: string[] = [];
  browserErrors.set(page, errors);
  const observe = (target: Page) => {
    target.on('pageerror', error => { errors.push(error.message); console.error('Browser script error:', error.message); });
    target.on('console', item => {
      if (item.type() === 'error' && item.text().includes('[ErrorBoundary]')) {
        errors.push(item.text()); console.error(item.text());
      }
    });
  };
  observe(page); context.on('page', observe);
});
test.afterEach(async ({ page }) => {
  expect(browserErrors.get(page)).toEqual([]);
});

async function login(page: Page, name = 'alice') {
  await page.goto('/login');
  await page.getByLabel('登录账号', { exact: true }).fill(name);
  await page.getByLabel('密码', { exact: true }).fill('correct-password');
  await page.getByRole('button', { name: '登 录' }).click();
  await expect(page).toHaveURL(/workspace/);
}
async function members(page: Page) {
  await page.getByRole('menuitem').filter({ hasText: '用户管理' }).click();
  await expect(page.getByText('成员列表', { exact: true })).toBeVisible();
}
async function chooseScope(page: Page, name: string, container: Page | Locator = page) {
  await container.getByRole('combobox', { name: '工作单位' }).press('ArrowDown');
  await page.locator('.ant-select-dropdown:visible').getByText(name, { exact: true }).click();
}
async function direct(page: Page, method: string, path: string, body?: unknown) {
  return page.evaluate(async ({ method, path, body }) => {
    const state = JSON.parse(sessionStorage.getItem('cyys.session') || '{}');
    const response = await fetch(path, { method, headers: { Authorization: `Bearer ${state.token}`, 'X-Cyys-Scope-Id': state.scopeId || '', 'Content-Type': 'application/json' }, body: body === undefined ? undefined : JSON.stringify(body) });
    return { status: response.status, body: await response.json() };
  }, { method, path, body });
}
test('真实登录、空查询、新增、详情、修改与角色授予；撤权清除页面', async ({ page, context }) => {
  await page.goto('/login');
  await expect(page.getByLabel('登录账号', { exact: true })).toBeVisible();
  await page.screenshot({ path: 'test-results/login.png' });
  await login(page); await members(page);
  await expect(page.getByRole('row').filter({ hasText: '单位 A 姓名' })).toBeVisible();
  await page.getByLabel('成员姓名', { exact: true }).fill('不存在的姓名');
  await page.getByRole('button', { name: '查 询' }).click();
  await expect(page.locator('.ant-empty-description')).toHaveText('暂无数据');
  await page.getByRole('button', { name: '重 置' }).click();
  await page.getByRole('button', { name: '新增成员' }).click();
  const dialog = page.getByRole('dialog', { name: '新增成员' });
  await expect(dialog).toBeVisible();
  await dialog.getByRole('button', { name: '确 定' }).click();
  await expect(dialog).toBeVisible();
  await dialog.getByLabel('登录账号', { exact: true }).fill('g4-browser');
  await dialog.getByLabel('初始密码', { exact: true }).fill('correct-password');
  await dialog.getByLabel('本单位姓名', { exact: true }).fill('浏览器成员');
  await dialog.getByLabel('部门', { exact: true }).click();
  await page.locator('.ant-select-tree-list-holder:visible').getByText('g1-a-child', { exact: true }).click();
  await dialog.getByRole('button', { name: '确 定' }).click();
  await expect(dialog).not.toBeVisible();
  const row = page.locator('tr[data-row-key]').filter({ hasText: 'g4-browser' });
  await expect(row).toContainText('浏览器成员');
  await row.getByText('详情', { exact: true }).click();
  await expect(page.getByRole('dialog').getByText('g1-a-child', { exact: true })).toBeVisible();
  await page.getByRole('dialog', { name: '成员详情' }).getByRole('button', { name: '关闭', exact: true }).click();
  await row.getByText('修改', { exact: true }).click();
  const edit = page.getByRole('dialog', { name: '修改成员' });
  await expect(edit.getByLabel('登录账号', { exact: true })).toHaveCount(0);
  await edit.getByLabel('本单位姓名', { exact: true }).fill('浏览器成员已修改');
  await edit.getByRole('button', { name: '确 定' }).click();
  await expect(edit).not.toBeVisible();
  await expect(row).toContainText('浏览器成员已修改');
  await row.getByText('角色', { exact: true }).click();
  const grant = page.getByRole('dialog', { name: '授予本单位角色' });
  await grant.getByLabel('单位角色', { exact: true }).click();
  await page.locator('.ant-select-dropdown:visible .ant-select-item-option-content').filter({ hasText: /^A_READER$/ }).click();
  await grant.getByLabel('单位角色', { exact: true }).press('Escape');
  await grant.getByRole('button', { name: '确 定' }).click();
  await expect(grant).not.toBeVisible();
  await page.screenshot({ path: 'test-results/members.png', fullPage: true });

  const other = await context.newPage();
  await login(other, 'g4-browser'); await members(other);
  await expect(other.getByRole('row').filter({ hasText: 'g4-browser' })).toBeVisible();
  const result = await direct(page, 'GET', '/api/admin/users?name=浏览器成员已修改');
  const id = result.body.data.records[0].id;
  expect((await direct(page, 'PUT', `/api/admin/users/${id}/roles`, [])).status).toBe(200);
  await other.evaluate(() => window.dispatchEvent(new Event('focus')));
  await expect(other.getByText('成员列表', { exact: true })).not.toBeVisible();
  expect((await direct(other, 'GET', '/api/admin/users')).status).toBe(403);
  await other.close();
});

test('两标签页单位独立、按钮随权限变化，隐藏操作直接调用仍拒绝', async ({ page, context }) => {
  await login(page); await members(page);
  const other = await context.newPage();
  await login(other); await members(other); await chooseScope(other, '单位 B');
  await expect(other.getByRole('button', { name: '新增成员' })).toHaveCount(0);
  await expect(other.getByRole('row').filter({ hasText: '单位 B 姓名' })).toHaveCount(2);
  await expect(other.getByText('修改', { exact: true })).toHaveCount(0);
  await expect(page.getByRole('row').filter({ hasText: '单位 A 姓名' })).toBeVisible();
  expect((await direct(other, 'POST', '/api/admin/users', { loginName: 'g4-hidden-browser', password: 'correct-password', displayName: '越权' })).status).toBe(403);
  expect((await direct(other, 'GET', '/api/admin/users/u-alice-scope-a')).status).toBe(404);
  await other.close();
});

test('切换提示未保存内容，切换后表单及选项清空', async ({ page }) => {
  await login(page); await members(page);
  await page.getByRole('button', { name: '新增成员' }).click();
  const dialog = page.getByRole('dialog', { name: '新增成员' });
  await dialog.getByLabel('本单位姓名', { exact: true }).fill('未保存内容');
  await Promise.all([
    page.waitForResponse(response => response.url().endsWith('/api/auth/me')),
    page.evaluate(() => window.dispatchEvent(new Event('focus'))),
  ]);
  await expect(dialog.getByLabel('本单位姓名', { exact: true })).toHaveValue('未保存内容');
  await chooseScope(page, '单位 B', dialog);
  await page.getByRole('button', { name: '继续编辑' }).click();
  await expect(dialog.getByLabel('本单位姓名', { exact: true })).toHaveValue('未保存内容');
  await chooseScope(page, '单位 B', dialog);
  await page.getByRole('button', { name: '切换并清除' }).click();
  await expect(dialog).not.toBeVisible();
  await expect(page.getByRole('button', { name: '新增成员' })).toHaveCount(0);
  await chooseScope(page, '单位 A');
  await page.getByRole('button', { name: '新增成员' }).click();
  await expect(dialog.getByLabel('本单位姓名', { exact: true })).toHaveValue('');
  await dialog.getByLabel('部门', { exact: true }).click();
  await expect(page.locator('.ant-select-dropdown:visible')).not.toContainText('g1-b-root');
});

test('单位 A 的真实迟到响应不能覆盖单位 B 的结果', async ({ page }) => {
  await login(page); await members(page);
  await expect(page.getByRole('row').filter({ hasText: '单位 A 姓名' })).toBeVisible();
  let release!: () => void;
  let held!: () => void;
  const barrier = new Promise<void>(resolve => { release = resolve; });
  const captured = new Promise<void>(resolve => { held = resolve; });
  await page.route('**/api/admin/users?*', async route => {
    if (route.request().headers()['x-cyys-scope-id'] === 'scope-a') {
      const response = await route.fetch(); held(); await barrier; await route.fulfill({ response });
    } else await route.continue();
  });
  await page.getByRole('img', { name: 'reload', exact: true }).click();
  await captured;
  await chooseScope(page, '单位 B');
  await expect(page.getByRole('row').filter({ hasText: '单位 B 姓名' })).toHaveCount(2);
  release();
  await page.unrouteAll({ behavior: 'wait' });
  await expect(page.getByRole('row').filter({ hasText: '单位 A 姓名' })).toHaveCount(0);
  await expect(page.getByRole('row').filter({ hasText: '单位 B 姓名' })).toHaveCount(2);
});

test('平台全部单位显式只读，选择单位也不自动取得写权限', async ({ page }) => {
  await login(page, 'platform'); await chooseScope(page, '全部单位 · 只读'); await members(page);
  await expect(page.getByText('全部单位只读视图', { exact: true })).toBeVisible();
  await expect(page.getByRole('button', { name: '新增成员' })).toHaveCount(0);
  await expect(page.getByRole('row').filter({ hasText: '单位 B 姓名' })).toHaveCount(2);
  await chooseScope(page, '单位 A');
  await expect(page.getByRole('row').filter({ hasText: '单位 A 姓名' })).toBeVisible();
  await expect(page.getByRole('button', { name: '新增成员' })).toHaveCount(0);
  expect((await direct(page, 'POST', '/api/admin/users', { loginName: 'g4-platform-browser', password: 'correct-password', displayName: '无写权限' })).status).toBe(403);
});

test('官方布局的主题设置在单位切换后保留', async ({ page }) => {
  await login(page);
  await expect(page.getByRole('link', { name: 'Ant Design Pro', exact: true })).toBeVisible();
  const handle = page.locator('.ant-pro-setting-drawer-handle');
  const dark = page.locator('.ant-pro-setting-drawer-block-checkbox-item-realDark');
  await handle.click();
  await dark.click();
  await expect(dark.getByRole('img', { name: 'check' })).toBeVisible();
  await handle.click();
  await chooseScope(page, '单位 B');
  await handle.click();
  await expect(dark.getByRole('img', { name: 'check' })).toBeVisible();
  await handle.click();
});

test('部门管理使用真实组织树候选并完成新增、修改、删除', async ({ page }) => {
  await login(page);
  await page.getByRole('menuitem').filter({ hasText: '部门管理' }).click();
  await expect(page.getByText('部门列表', { exact: true })).toBeVisible();
  await expect(page.locator('tr[data-row-key="g1-a-root"]')).toBeVisible();
  await expect(page.getByText('g1-b-root', { exact: true })).toHaveCount(0);

  await page.getByRole('button', { name: '新增部门' }).click();
  const create = page.getByRole('dialog', { name: '新增部门' });
  await create.getByRole('button', { name: '确 定' }).click();
  await expect(create).toBeVisible();
  await create.getByLabel('部门名称', { exact: true }).fill('浏览器部门');
  await create.getByLabel('部门编码', { exact: true }).fill('browser-department');
  await create.getByLabel('上级部门', { exact: true }).click();
  await page.locator('.ant-select-tree-list-holder:visible').getByText('g1-a-root', { exact: true }).click();
  await create.getByRole('button', { name: '确 定' }).click();
  await expect(create).not.toBeVisible();

  let row = page.locator('tr[data-row-key]').filter({ hasText: '浏览器部门' });
  await expect(row).toContainText('g1-a-root');
  await row.getByText('修改', { exact: true }).click();
  const edit = page.getByRole('dialog', { name: '修改部门' });
  await edit.getByLabel('部门名称', { exact: true }).fill('浏览器部门已修改');
  await edit.getByRole('button', { name: '确 定' }).click();
  await expect(edit).not.toBeVisible();

  row = page.locator('tr[data-row-key]').filter({ hasText: '浏览器部门已修改' });
  await row.getByText('删除', { exact: true }).click();
  await page.getByRole('button', { name: '删 除' }).click();
  await expect(row).toHaveCount(0);
});

test('角色管理按授予上限配置菜单权限和自定义部门范围', async ({ page }) => {
  await login(page);
  await page.getByRole('menuitem').filter({ hasText: '角色管理' }).click();
  await expect(page.getByText('角色列表', { exact: true })).toBeVisible();
  await expect(page.locator('tr[data-row-key="role-a"]')).toBeVisible();
  await expect(page.getByText('B_WRITER', { exact: true })).toHaveCount(0);

  await page.getByRole('button', { name: '新增角色' }).click();
  const create = page.getByRole('dialog', { name: '新增角色' });
  await create.getByLabel('角色编码', { exact: true }).fill('G4_BROWSER_ROLE');
  await create.getByLabel('角色名称', { exact: true }).fill('浏览器角色');
  await create.getByText('自定义部门', { exact: true }).first().click();
  await create.locator('.ant-tree-treenode').filter({ hasText: 'g1-a-child' }).locator('.ant-tree-checkbox').click();
  await create.locator('.ant-tree-treenode').filter({ hasText: 'test:read（test:read）' }).locator('.ant-tree-checkbox').click();
  await create.getByRole('button', { name: '确 定' }).click();
  await expect(create).not.toBeVisible();

  let row = page.locator('tr[data-row-key]').filter({ hasText: '浏览器角色' });
  await expect(row).toContainText('自定义部门');
  await row.getByText('修改', { exact: true }).click();
  const edit = page.getByRole('dialog', { name: '修改角色' });
  await edit.getByLabel('角色名称', { exact: true }).fill('浏览器角色已修改');
  await edit.getByText('本单位全部', { exact: true }).click();
  await edit.getByRole('button', { name: '确 定' }).click();
  await expect(edit).not.toBeVisible();

  row = page.locator('tr[data-row-key]').filter({ hasText: '浏览器角色已修改' });
  await expect(row).toContainText('本单位全部');
  await row.getByText('删除', { exact: true }).click();
  await page.getByRole('button', { name: '删 除' }).click();
  await expect(row).toHaveCount(0);
});
