import type { CreateMember, DepartmentOption, EditMember, Member, MemberDetail, Option, Page } from '@/types/api';
import { api } from '@/utils/request';
import { session } from '@/utils/session';

const root = '/api/admin/users';
export const listMembers = (page: number, size: number, name?: string) => {
  const all = session().allUnits;
  const query = new URLSearchParams({ page: String(page), size: String(size) });
  if (name && !all) query.set('name', name);
  return api<Page<Member>>(`${root}${all ? '/all' : ''}?${query}`);
};
export const memberDetail = (id: string) => api<MemberDetail>(`${root}/${encodeURIComponent(id)}`);
export const createMember = (body: CreateMember) => api<Member>(root, { method: 'POST', body });
export const editMember = (id: string, body: EditMember) => api<Member>(`${root}/${encodeURIComponent(id)}`, { method: 'PUT', body });
export const grantRoles = (id: string, body: string[]) => api<void>(`${root}/${encodeURIComponent(id)}/roles`, { method: 'PUT', body });
export const departmentOptions = () => api<DepartmentOption[]>(`${root}/departments`);
export const roleOptions = () => api<Option[]>(`${root}/roles`);
