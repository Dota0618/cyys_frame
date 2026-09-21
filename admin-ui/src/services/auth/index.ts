import type { LoginResult, UserInfo } from '@/types/api';
import { api, ApiError } from '@/utils/request';
import { session, updateSession } from '@/utils/session';
export const login = (body: { loginName: string; password: string }) => api<LoginResult>('/api/auth/login', { method: 'POST', body, anonymous: true, quiet: true });
export const currentUser = async () => {
  let user: UserInfo;
  try {
    user = await api<UserInfo>('/api/auth/me', { quiet: true });
  } catch (error) {
    if (!(error instanceof ApiError) || error.status !== 403 || !session().scopeId) throw error;
    // 仅刷新身份时去掉失效的筛选；业务请求仍拒绝越界，绝不自动重试写入。
    user = await api<UserInfo>('/api/auth/me', { quiet: true, omitScope: true });
  }
  const scopeId = user.currentScopeId || user.defaultScope?.id || user.scopes[0]?.id;
  if (session().scopeId !== scopeId) updateSession({ token: session().token, scopeId });
  return { ...user, currentScopeId: scopeId };
};
export const logout = () => api<void>('/api/auth/logout', { method: 'POST', quiet: true });
export const changePassword = (body: { currentPassword: string; newPassword: string }) =>
  api<void>('/api/auth/password', { method: 'POST', body });
export const resetAccountPassword = (id: string, body: { newPassword: string }) =>
  api<void>(`/api/admin/platform/accounts/${encodeURIComponent(id)}/password`, { method: 'POST', body });
