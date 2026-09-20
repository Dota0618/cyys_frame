import { test, expect } from '@playwright/test';
import { login, direct, members, chooseScope } from './browser-support';

test('个人改密与管理员重置只要求目标新密码，旧会话失效', async ({ page, context }) => {
  await login(page);
  const created = await direct(page, 'POST', '/api/admin/users', {
    loginName: 'g4-password-browser', password: 'correct-password', displayName: '验收密码账号',
  });
  expect(created.status).toBe(200);
  const personal = await context.newPage();
  await login(personal, 'g4-password-browser');
  const oldSession = await context.newPage();
  await login(oldSession, 'g4-password-browser');
  const identity = (await direct(personal, 'GET', '/api/auth/me')).body.data;
  await personal.getByText(identity.displayName, { exact: true }).first().hover();
  await personal.getByRole('menuitem', { name: '修改密码' }).click();
  let form = personal.getByRole('dialog', { name: '修改密码', exact: true });
  await form.getByLabel('原密码', { exact: true }).fill('correct-password');
  await form.getByLabel('新密码', { exact: true }).fill('changed-password-2026');
  await form.getByLabel('确认新密码', { exact: true }).fill('different-password');
  await form.getByRole('button', { name: '确 定' }).click();
  await expect(form.getByText('两次输入的密码不一致')).toBeVisible();
  await form.getByLabel('确认新密码', { exact: true }).fill('changed-password-2026');
  await form.getByRole('button', { name: '确 定' }).click();
  await expect(personal).toHaveURL(/login/);
  expect((await direct(oldSession, 'GET', '/api/auth/me')).status).toBe(401);
  await login(personal, 'g4-password-browser', 'changed-password-2026');

  await page.bringToFront();
  await login(page, 'platform'); await chooseScope(page, '单位 A'); await members(page);
  await page.getByRole('row').filter({ hasText: 'g4-password-browser' }).getByText('重置密码', { exact: true }).click();
  form = page.getByRole('dialog', { name: '重置密码：g4-password-browser' });
  await expect(form.locator('input[type=password]')).toHaveCount(2);
  await expect(form.getByLabel('原密码', { exact: true })).toHaveCount(0);
  await form.getByLabel('新密码', { exact: true }).fill('reset-password-2026');
  await form.getByLabel('确认新密码', { exact: true }).fill('reset-password-2026');
  await form.getByRole('button', { name: '确 定' }).click();
  await expect(form).not.toBeVisible();
  expect((await direct(personal, 'GET', '/api/auth/me')).status).toBe(401);
  await login(personal, 'g4-password-browser', 'reset-password-2026');
  await personal.close(); await oldSession.close();
});
