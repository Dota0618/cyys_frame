import type { Page } from '@/types/api';
import { api } from '@/utils/request';

const root = '/api/admin/recycle';
export interface RecycleResource {
  code: string; name: string; searchLabel: string; description: string;
  scopeRequired: boolean; canRestore: boolean; canPurge: boolean;
}
export interface RecycleRecord {
  id: string; title: string; scopeId?: string; scopeName?: string;
  departmentId?: string; departmentName?: string; deletedBy: string;
  deletedByName?: string; deletedAt: string; createdAt: string; version: number;
}
export const recycleResources = () => api<RecycleResource[]>(`${root}/resources`);
export const listRecycled = (resource: string, page: number, size: number, search?: string) =>
  api<Page<RecycleRecord>>(`${root}/${encodeURIComponent(resource)}?${new URLSearchParams({
    page: String(page), size: String(size), search: search || '',
  })}`);
export const changeRecycled = (resource: string, action: 'restore' | 'purge', records: RecycleRecord[]) =>
  api<void>(`${root}/${encodeURIComponent(resource)}/${action}`, {
    method: 'POST', body: { records: records.map(({ id, version }) => ({ id, version })) },
  });
