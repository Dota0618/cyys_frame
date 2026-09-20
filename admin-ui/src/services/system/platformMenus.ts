import type { CreatePlatformMenu, EditPlatformMenu, PlatformMenu } from '@/types/api';
import { api } from '@/utils/request';

const root = '/api/admin/platform/menus';

export const listPlatformMenus = () => api<PlatformMenu[]>(root);
export const platformMenuDetail = (id: string) =>
  api<PlatformMenu>(`${root}/${encodeURIComponent(id)}`);
export const createPlatformMenu = (body: CreatePlatformMenu) =>
  api<PlatformMenu>(root, { method: 'POST', body });
export const editPlatformMenu = (body: EditPlatformMenu) =>
  api<PlatformMenu>(root, { method: 'PUT', body });
export const deletePlatformMenu = (id: string, version: number) =>
  api<void>(`${root}/${encodeURIComponent(id)}?version=${version}`, { method: 'DELETE' });
