import type { Configuration, CreateConfiguration, UpdateConfiguration, Page } from '@/types/api';
import { api } from '@/utils/request';

const root = '/api/admin/platform/configurations';

export const listConfigurations = (page: number, size: number, name = '') =>
  api<Page<Configuration>>(`${root}?page=${page}&size=${size}&name=${encodeURIComponent(name)}`);
export const configurationDetail = (id: string) =>
  api<Configuration>(`${root}/${encodeURIComponent(id)}`);
export const createConfiguration = (body: CreateConfiguration) =>
  api<Configuration>(root, { method: 'POST', body });
export const updateConfiguration = (id: string, body: UpdateConfiguration) =>
  api<Configuration>(`${root}/${encodeURIComponent(id)}`, { method: 'PUT', body });
export const deleteConfiguration = (id: string, version: number) =>
  api<void>(`${root}/${encodeURIComponent(id)}?version=${version}`, { method: 'DELETE' });
