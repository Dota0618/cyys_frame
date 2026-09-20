import type { Page } from '@/types/api';
import { api, apiBlob, type Options } from '@/utils/request';

const root = '/api/admin/files';
export interface ManagedFile { id: string; name: string; byteSize?: number; state: string; version: number; createdAt: string; resumable: boolean; ownUpload: boolean; directoryId?: string }
export interface FileDirectory { id: string; name: string; version: number }
export interface UploadSession { id: string; name: string; bytes: number; partBytes: number; state: string; expiresAt: string; completed: number[]; hashes: string[] }
export interface UploadLimits { directBytes: number; resumableBytes: number; partBytes: number }
export const listFiles = (page: number, size: number, search?: string, directoryId?: string) => {
  const query = new URLSearchParams({ page: String(page), size: String(size), search: search || '' });
  if (directoryId) query.set('directoryId', directoryId);
  return api<Page<ManagedFile>>(`${root}?${query}`);
};
const references = (files: ManagedFile[]) => files.map(({ id, version }) => ({ id, version }));
export const listDirectories = () => api<FileDirectory[]>('/api/admin/file-directories');
export const moveDirectoryOptions = () => api<FileDirectory[]>('/api/admin/file-directories/move-options');
export const createDirectory = (name: string) => api<FileDirectory>('/api/admin/file-directories', { method: 'POST', body: { name } });
export const renameDirectory = (directory: FileDirectory, name: string) => api<FileDirectory>(`/api/admin/file-directories/${encodeURIComponent(directory.id)}`, { method: 'PUT', body: { name, version: directory.version } });
export const deleteDirectory = (directory: FileDirectory) => api<void>(`/api/admin/file-directories/${encodeURIComponent(directory.id)}?version=${directory.version}`, { method: 'DELETE' });
export const moveFiles = (files: ManagedFile[], directoryId?: string) => api<void>(`${root}/move`, { method: 'POST', body: { files: references(files), directoryId: directoryId || null } });
export const trashFiles = (files: ManagedFile[]) => api<void>(`${root}/trash`, { method: 'POST', body: references(files) });
export const listRecycledFiles = (page: number, size: number, search?: string) => api<Page<ManagedFile>>(`${root}/recycle?${new URLSearchParams({ page: String(page), size: String(size), search: search || '' })}`);
export const restoreFiles = (files: ManagedFile[]) => api<void>(`${root}/restore`, { method: 'POST', body: references(files) });
export const purgeFile = (file: ManagedFile) => api<void>(`${root}/recycle/${encodeURIComponent(file.id)}?version=${file.version}`, { method: 'DELETE' });
export const renameFile = (id: string, name: string, version: number) => api<ManagedFile>(`${root}/${encodeURIComponent(id)}`, { method: 'PUT', body: { name, version } });
export const trashFile = (file: ManagedFile) => api<void>(`${root}/${encodeURIComponent(file.id)}?version=${file.version}`, { method: 'DELETE' });
export const downloadFile = (id: string) => apiBlob(`${root}/${encodeURIComponent(id)}/content`);
export const uploadLimits = (options?: Options) => api<UploadLimits>(`${root}/uploads/limits`, options);
export const uploadProgress = (id: string, options?: Options) => api<UploadSession>(`${root}/uploads/${encodeURIComponent(id)}`, options);
export const startUpload = (name: string, bytes: number, hashes: string[], options: Options, directoryId?: string) => api<UploadSession>(`${root}/uploads`, { ...options, method: 'POST', body: { name, bytes, hashes, directoryId } });
export const completeUpload = (id: string, options: Options) => api<ManagedFile>(`${root}/uploads/${encodeURIComponent(id)}/complete`, { ...options, method: 'POST' });
export const cancelUpload = (id: string) => api<void>(`${root}/uploads/${encodeURIComponent(id)}/cancel`, { method: 'POST' });
export const cleanupUpload = (id: string, options?: Options) => api<void>(`${root}/uploads/${encodeURIComponent(id)}/cleanup`, { ...options, method: 'POST' });
export function uploadFile(file: File, options: Options, directoryId?: string) {
  const body = new FormData(); body.append('file', file);
  return api<ManagedFile>(directoryId ? `${root}?${new URLSearchParams({ directoryId })}` : root, { ...options, method: 'POST', body });
}
export function uploadPart(id: string, index: number, blob: Blob, options: Options) {
  const body = new FormData(); body.append('file', blob, 'part');
  return api<UploadSession>(`${root}/uploads/${encodeURIComponent(id)}/parts/${index}`, { ...options, method: 'PUT', body });
}
