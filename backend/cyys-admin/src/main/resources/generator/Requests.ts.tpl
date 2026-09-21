import { api } from '@/utils/request';
import type { Page, DepartmentOption } from '@/types/api';
import type { DictionaryChoice } from '@/components/DictionarySelect';

export interface @@CLASS@@Command {
  @@TS_FIELDS@@
}
export interface @@CLASS@@Row extends @@CLASS@@Command {
  id: string; departmentId: string; version: number; createdAt: string;
  @@PARENT_TS@@
}
export interface @@CLASS@@Options { departments: DepartmentOption[]; parents: @@CLASS@@Row[]; dictionaries: Record<string, DictionaryChoice[]> }
const base = '/api/business/@@FEATURE@@';
export async function list(params: Record<string, unknown>) {
  const query = new URLSearchParams({ page: String(params.current || 1), size: String(params.pageSize || 20) });
  @@SEARCH_TS@@
  return api<Page<@@CLASS@@Row>>(`${base}?${query}`);
}
export const choices = () => api<@@CLASS@@Row[]>(`${base}/choices`);
export const options = (id?: string) => api<@@CLASS@@Options>(id ? `${base}/${id}/update-options` : `${base}/create-options`);
export const create = (command: @@CLASS@@Command & { departmentId: string; @@PARENT_TS@@ }) => api<@@CLASS@@Row>(base, { method: 'POST', body: command });
export const update = (id: string, command: @@CLASS@@Command & { version: number }) => api<@@CLASS@@Row>(`${base}/${id}`, { method: 'PUT', body: command });
export const remove = (row: @@CLASS@@Row) => api<void>(`${base}/${row.id}?version=${row.version}`, { method: 'DELETE' });
