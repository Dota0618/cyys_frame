import { api, apiBlob, type Options } from '@/utils/request';
import type { Page, DepartmentOption } from '@/types/api';

export interface DictionaryOption { value: string; label: string; disabled: boolean }
export interface Attachment { id: string; name: string; byteSize?: number; available?: boolean }
export interface OrgNotice {
  id: string; departmentId: string; departmentName?: string; title: string;
  type?: string; typeLabel?: string; typeDisabled?: boolean; version: number;
  readCount: number; read: boolean; createdAt: string;
}
export interface NoticeDetail { notice: OrgNotice; content: string; attachments: Attachment[] }
export interface NoticeOptions { departments: DepartmentOption[]; types: DictionaryOption[] }
export interface NoticeCommand { title: string; type?: string | null; content: string; attachments: string[] }
export interface NoticeReader { userId: string; userName?: string; firstReadAt: string }
const base = '/api/admin/org-notices';
const paging = (page: number, size: number, name: string, search?: string) => {
  const query = new URLSearchParams({ page: String(page), size: String(size) }); if (search) query.set(name, search); return query;
};
export const listNotices = (page: number, size: number, title?: string) => api<Page<OrgNotice>>(`${base}?${paging(page, size, 'title', title)}`);
export const noticeDetail = (id: string, options?: Options) => api<NoticeDetail>(`${base}/${id}`, options);
export const noticeEdit = (id: string) => api<NoticeDetail>(`${base}/${id}/edit`);
export const noticeOptions = (id?: string) => api<NoticeOptions>(id ? `${base}/${id}/update-options` : `${base}/create-options`);
export const noticeFiles = (id: string | undefined, page: number, size: number, search?: string) =>
  api<Page<Attachment>>(`${id ? `${base}/${id}/update-files` : `${base}/create-files`}?${paging(page, size, 'search', search)}`);
export const createNotice = (command: NoticeCommand & { departmentId: string }) => api<NoticeDetail>(base, { method: 'POST', body: command });
export const updateNotice = (id: string, command: NoticeCommand & { version: number }) => api<NoticeDetail>(`${base}/${id}`, { method: 'PUT', body: command });
export const removeNotice = (notice: OrgNotice) => api<void>(`${base}/${notice.id}?version=${notice.version}`, { method: 'DELETE' });
export const readNotice = (id: string, options?: Options) => api<void>(`${base}/${id}/read`, { ...options, method: 'POST' });
export const noticeReaders = (id: string, page: number, size: number) => api<Page<NoticeReader>>(`${base}/${id}/readers?page=${page}&size=${size}`);
export const noticeAttachment = (id: string, file: string) => apiBlob(`${base}/${id}/attachments/${file}/content`);
