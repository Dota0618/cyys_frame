import type { LoginResult, UserInfo } from '@/types/api';
import { api } from '@/utils/request';
export const login = (body: { loginName: string; password: string }) => api<LoginResult>('/api/auth/login', { method: 'POST', body, anonymous: true, quiet: true });
export const currentUser = () => api<UserInfo>('/api/auth/me', { quiet: true });
export const logout = () => api<void>('/api/auth/logout', { method: 'POST', quiet: true });
