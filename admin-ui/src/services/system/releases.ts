import type { DictionaryChoice } from '@/components/DictionarySelect';
import type { FileChoice } from '@/components/FilePicker';
import type { Page } from '@/types/api';
import { api, apiBlob } from '@/utils/request';

export interface ReleaseCommand {
  releaseNumber: string; dateUpdate: string; type: string; content: string; attachments: string[];
}
export interface Release {
  id: string; releaseNumber: string; dateUpdate: string; type: string; typeLabel: string;
  typeDisabled: boolean; content: string; version: number; createdAt: string;
}
export interface ReleaseDetail { release: Release; attachments: FileChoice[] }
const root = '/api/admin/platform/releases';
export const listReleases = (page: number, size: number, number = '') =>
  api<Page<Release>>(`${root}?page=${page}&size=${size}&number=${encodeURIComponent(number)}`);
export const releaseDetail = (id: string) => api<ReleaseDetail>(`${root}/${encodeURIComponent(id)}`);
export const releaseOptions = (id?: string) => api<DictionaryChoice[]>(id ? `${root}/${encodeURIComponent(id)}/edit-options` : `${root}/create-options`);
export const releaseFiles = (id: string | undefined, page: number, size: number, search = '') =>
  api<Page<FileChoice>>(`${id ? `${root}/${encodeURIComponent(id)}/edit-files` : `${root}/create-files`}?page=${page}&size=${size}&search=${encodeURIComponent(search)}`);
export const createRelease = (body: ReleaseCommand) => api<ReleaseDetail>(root, { method: 'POST', body });
export const updateRelease = (id: string, body: ReleaseCommand & { version: number }) =>
  api<ReleaseDetail>(`${root}/${encodeURIComponent(id)}`, { method: 'PUT', body });
export const deleteRelease = (id: string, version: number) => api<void>(`${root}/${encodeURIComponent(id)}?version=${version}`, { method: 'DELETE' });
export const releaseAttachment = (id: string, file: string) => apiBlob(`${root}/${encodeURIComponent(id)}/attachments/${encodeURIComponent(file)}`);
