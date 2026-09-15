import type {
  CreateDepartment,
  Department,
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
export const createDepartment = (body: CreateDepartment) =>
  api<Department>(root, { method: 'POST', body });
export const editDepartment = (body: EditDepartment) =>
  api<Department>(root, { method: 'PUT', body });
export const deleteDepartment = (id: string, version: number) =>
  api<void>(root, { method: 'DELETE', body: [{ id, version }] });
