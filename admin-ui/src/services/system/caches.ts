import { api } from '@/utils/request';

export type CacheScope = 'SYSTEM' | 'UNIT';

export interface CacheDefinition {
  id: string;
  name: string;
  cacheKey: string;
  groupName: string;
  refreshGroup: string;
  scope: CacheScope;
  description?: string;
  version: number;
}

export interface CreateCacheDefinition {
  name: string;
  cacheKey: string;
  groupName: string;
  refreshGroup: string;
  scope: CacheScope;
  description?: string;
}

export interface UpdateCacheDefinition extends CreateCacheDefinition {
  version: number;
}

export const listCacheDefinitions = () => api<CacheDefinition[]>('/api/admin/caches');
export const createCacheDefinition = (body: CreateCacheDefinition) =>
  api<CacheDefinition>('/api/admin/caches', { method: 'POST', body });
export const updateCacheDefinition = (id: string, body: UpdateCacheDefinition) =>
  api<CacheDefinition>(`/api/admin/caches/${encodeURIComponent(id)}`, { method: 'PUT', body });
export const deleteCacheDefinition = (id: string, version: number) =>
  api<void>(`/api/admin/caches/${encodeURIComponent(id)}?version=${version}`, { method: 'DELETE' });
export const refreshCacheGroup = (body: { refreshGroup: string; scope: CacheScope }) =>
  api<void>('/api/admin/caches/refresh-groups/refresh', { method: 'POST', body });
