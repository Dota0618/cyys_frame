import type { CreateMember, DepartmentOption, EditMember, Member, MemberDetail, Option, Page } from '@/types/api';
import { api, apiBlob } from '@/utils/request';
import { session } from '@/utils/session';

const root = '/api/admin/users';
export const exportMembers = (name: string) => apiBlob(`${root}/export`, { method: 'POST', body: { name } });
export const listMembers = (page: number, size: number, name?: string) => {
  const query = new URLSearchParams({ page: String(page), size: String(size) });
  if (name) query.set('name', name);
  return api<Page<Member>>(`${root}?${query}`);
};
export const memberDetail = (id: string) => api<MemberDetail>(`${root}/${encodeURIComponent(id)}`);
export const createMember = (body: CreateMember) => api<Member>(root, { method: 'POST', body });
export const editMember = (id: string, body: EditMember) => api<Member>(`${root}/${encodeURIComponent(id)}`, { method: 'PUT', body });
export const grantRoles = (id: string, body: string[]) => api<void>(`${root}/${encodeURIComponent(id)}/roles`, { method: 'PUT', body });
export const departmentOptions = () => api<DepartmentOption[]>(`${root}/departments`);
export const roleOptions = () => api<Option[]>(`${root}/roles`);

export const memberDataPermission = (id: string) => api<import('@/types/api').MemberDataPermission>(`/api/admin/users/${encodeURIComponent(id)}/data-permission`);
export const updateMemberDataPermission = (id: string, body: import('@/types/api').UpdateMemberDataPermission) =>
  api<import('@/types/api').MemberDataPermission>(`/api/admin/users/${encodeURIComponent(id)}/data-permission`, { method: 'PUT', body });
