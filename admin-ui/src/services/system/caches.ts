import { api } from '@/utils/request';

export interface CacheAction { id: string; name: string; scope: string; contents: string[]; description: string }
export interface CacheRefreshItem { id: string; name: string; version: number }
export interface CreateCacheRefreshItem { name: string }
export interface UpdateCacheRefreshItem { name: string; version: number }

export const listCacheActions = () => api<CacheAction[]>('/api/admin/caches');
export const refreshCache = (id: string) => api<void>(`/api/admin/caches/${encodeURIComponent(id)}/refresh`, { method: 'POST' });
export const listCacheRefreshItems = () => api<CacheRefreshItem[]>('/api/admin/caches/refresh-items');
export const createCacheRefreshItem = (body: CreateCacheRefreshItem) =>
  api<CacheRefreshItem>('/api/admin/caches/refresh-items', { method: 'POST', body });
export const updateCacheRefreshItem = (id: string, body: UpdateCacheRefreshItem) =>
  api<CacheRefreshItem>(`/api/admin/caches/refresh-items/${encodeURIComponent(id)}`, { method: 'PUT', body });
export const deleteCacheRefreshItem = (id: string, version: number) =>
  api<void>(`/api/admin/caches/refresh-items/${encodeURIComponent(id)}?version=${version}`, { method: 'DELETE' });
export const refreshConfiguredCache = (id: string) =>
  api<void>(`/api/admin/caches/refresh-items/${encodeURIComponent(id)}/refresh`, { method: 'POST' });
