import type {
  CreateRole,
  EditRole,
  Page,
  Role,
  RoleDetail,
  RoleOptions,
} from '@/types/api';
import { api } from '@/utils/request';

const root = '/api/admin/roles';

export const listRoles = (page: number, size: number, search?: string) => {
  const query = new URLSearchParams({ page: String(page), size: String(size) });
  if (search) query.set('search', search);
  return api<Page<Role>>(`${root}?${query}`);
};
export const roleDetail = (id: string) =>
  api<RoleDetail>(`${root}/${encodeURIComponent(id)}`);
export const roleCreateOptions = () => api<RoleOptions>(`${root}/create-options`);
export const roleUpdateOptions = () => api<RoleOptions>(`${root}/update-options`);
export const createRole = (body: CreateRole) =>
  api<Role>(root, { method: 'POST', body });
export const editRole = (body: EditRole) =>
  api<Role>(root, { method: 'PUT', body });
export const deleteRole = (id: string, version: number) =>
  api<void>(`${root}/${encodeURIComponent(id)}?version=${version}`, { method: 'DELETE' });
