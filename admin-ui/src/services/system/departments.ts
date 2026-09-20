import type {
  CreateDepartment,
  Department,
  DepartmentLeaderOption,
  DepartmentOption,
  EditDepartment,
  Page,
} from '@/types/api';
import { api } from '@/utils/request';

const root = '/api/admin/departments';

export const listDepartments = (page: number, size: number, search?: string) => {
  const query = new URLSearchParams({ page: String(page), size: String(size) });
  if (search) query.set('search', search);
  return api<Page<Department>>(`${root}?${query}`);
};
export const departmentDetail = (id: string) =>
  api<Department>(`${root}/${encodeURIComponent(id)}`);
export const departmentParentOptions = () =>
  api<DepartmentOption[]>(`${root}/parents`);
export const departmentTreeOptions = () =>
  api<DepartmentOption[]>(`${root}/tree`);
const leaderOptionsPath = (path: string, search?: string) => {
  const query = new URLSearchParams();
  if (search) query.set('search', search);
  const suffix = query.size ? `?${query}` : '';
  return api<DepartmentLeaderOption[]>(`${root}/${path}${suffix}`);
};
export const createDepartmentLeaderOptions = (search?: string) =>
  leaderOptionsPath('leader-options/create', search);
export const updateDepartmentLeaderOptions = (id: string, search?: string) =>
  leaderOptionsPath(`${encodeURIComponent(id)}/leader-options`, search);
export const createDepartment = (body: CreateDepartment) =>
  api<Department>(root, { method: 'POST', body });
export const editDepartment = (body: EditDepartment) =>
  api<Department>(root, { method: 'PUT', body });
export const deleteDepartment = (id: string, version: number) =>
  api<void>(root, { method: 'DELETE', body: [{ id, version }] });
