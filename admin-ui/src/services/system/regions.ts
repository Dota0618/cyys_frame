import type { Page } from '@/types/api';
import { api } from '@/utils/request';

export interface RegionFields {
  cityCode: string; name: string; shortName?: string | null; zipCode?: string | null;
  longitude?: number | null; latitude?: number | null; sort?: number;
}
export interface Region extends RegionFields { id: string; parentId?: string; level: number; sort: number; version: number; hasChildren: boolean }
const root = '/api/admin/platform/regions';
export const listRegions = (page: number, size: number, parentId?: string, name = '') =>
  api<Page<Region>>(`${root}?page=${page}&size=${size}&parentId=${encodeURIComponent(parentId || '')}&name=${encodeURIComponent(name)}`);
export const regionDetail = (id: string) => api<Region>(`${root}/${encodeURIComponent(id)}`);
export const createRegion = (body: RegionFields & { parentId?: string }) => api<Region>(root, { method: 'POST', body });
export const updateRegion = (id: string, body: RegionFields & { version: number }) => api<Region>(`${root}/${encodeURIComponent(id)}`, { method: 'PUT', body });
export const deleteRegion = (row: Region) => api<void>(`${root}/${encodeURIComponent(row.id)}?version=${row.version}`, { method: 'DELETE' });
export const deleteRegions = (rows: Region[]) => api<void>(`${root}/delete`, {
  method: 'POST', body: rows.map(({ id, version }) => ({ id, version })),
});
