import { api } from '@/utils/request';
import type { Page } from '@/types/api';

export interface ScopeRow { id: string; code: string; name: string; parentId: string; ancestors: string; shortName?: string; contactPerson?: string; contactPhone?: string; enabled: boolean; sort: number; remark?: string; version: number }
export interface CreateScope { code: string; name: string; parentId: string; shortName?: string; contactPerson?: string; contactPhone?: string; remark?: string }
export interface UpdateScope extends Omit<CreateScope, 'code'> { version: number; enabled: boolean; sort: number }
const base = '/api/admin/platform/scopes';
export const scopeTree = () => api<ScopeRow[]>(`${base}/tree`);
export const listScopes = (page: number, size: number, search?: string) => api<Page<ScopeRow>>(`${base}?${new URLSearchParams({ page: String(page), size: String(size), search: search || '' })}`);
export const createScope = (body: CreateScope) => api<ScopeRow>(base, { method: 'POST', body });
export const updateScope = (id: string, body: UpdateScope) => api<ScopeRow>(`${base}/${encodeURIComponent(id)}`, { method: 'PUT', body });
