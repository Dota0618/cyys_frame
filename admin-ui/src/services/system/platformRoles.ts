import type {
  CreatePlatformRole,
  EditPlatformRole,
  Page,
  PlatformAccount,
  PlatformRole,
  PlatformRoleDetail,
  PlatformRoleOptions,
} from '@/types/api';
import { api } from '@/utils/request';

const root = '/api/admin/platform/roles';
const query = (page: number, size: number, search?: string) => {
  const params = new URLSearchParams({ page: String(page), size: String(size) });
  if (search) params.set('search', search);
  return params;
};

export const listPlatformRoles = (page: number, size: number, search?: string) =>
  api<Page<PlatformRole>>(`${root}?${query(page, size, search)}`);
export const platformRoleDetail = (id: string) =>
  api<PlatformRoleDetail>(`${root}/${encodeURIComponent(id)}`);
export const platformRoleCreateOptions = () => api<PlatformRoleOptions>(`${root}/create-options`);
export const platformRoleUpdateOptions = () => api<PlatformRoleOptions>(`${root}/update-options`);
export const createPlatformRole = (body: CreatePlatformRole) =>
  api<PlatformRole>(root, { method: 'POST', body });
export const editPlatformRole = (body: EditPlatformRole) =>
  api<PlatformRole>(root, { method: 'PUT', body });
export const deletePlatformRole = (id: string, version: number) =>
  api<void>(`${root}/${encodeURIComponent(id)}?version=${version}`, { method: 'DELETE' });
export const listPlatformAccounts = (page: number, size: number, search?: string) =>
  api<Page<PlatformAccount>>(`${root}/accounts?${query(page, size, search)}`);
export const assignPlatformRoles = (userId: string, roleIds: string[]) =>
  api<void>(`${root}/accounts/${encodeURIComponent(userId)}/roles`, {
    method: 'PUT', body: { roleIds },
  });
