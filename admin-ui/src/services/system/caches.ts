import { api } from '@/utils/request';

export interface CacheAction { id: string; name: string; description: string }

export const listCacheActions = () => api<CacheAction[]>('/api/admin/caches');
export const refreshCache = (id: string) => api<void>(`/api/admin/caches/${encodeURIComponent(id)}/refresh`, { method: 'POST' });
