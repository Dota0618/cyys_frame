import type { DictionaryChoice } from '@/components/DictionarySelect';
import type { Page } from '@/types/api';
import { api } from '@/utils/request';

export interface SystemNoticeCommand { title: string; type: string; content: string; visible: boolean }
export interface SystemNotice extends SystemNoticeCommand {
  id: string; typeLabel?: string; typeDisabled?: boolean; version: number; createdAt: string;
}
const root = '/api/admin/platform/system-notices';
export const listSystemNotices = (page: number, size: number, title = '') =>
  api<Page<SystemNotice>>(`${root}?page=${page}&size=${size}&title=${encodeURIComponent(title)}`);
export const systemNoticeDetail = (id: string) => api<SystemNotice>(`${root}/${encodeURIComponent(id)}`);
export const systemNoticeOptions = (id?: string) =>
  api<DictionaryChoice[]>(id ? `${root}/${encodeURIComponent(id)}/edit-options` : `${root}/create-options`);
export const createSystemNotice = (body: SystemNoticeCommand) => api<SystemNotice>(root, { method: 'POST', body });
export const updateSystemNotice = (id: string, body: SystemNoticeCommand & { version: number }) =>
  api<SystemNotice>(`${root}/${encodeURIComponent(id)}`, { method: 'PUT', body });
export const deleteSystemNotice = (id: string, version: number) =>
  api<void>(`${root}/${encodeURIComponent(id)}?version=${version}`, { method: 'DELETE' });
