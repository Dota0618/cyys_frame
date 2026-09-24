import { api } from '@/utils/request';

const root = '/api/admin/platform/recycle-resources';
export interface RecycleColumnOption { name: string; type: string; nullable: boolean }
export interface RecycleTableOption { tableName: string; columns: RecycleColumnOption[] }
export interface RecycleConfiguration {
  id: string; code: string; name: string; tableName: string; titleColumn: string; searchColumn: string;
  scopeRequired: boolean; scopeColumn?: string; departmentColumn?: string; ancestorsColumn?: string; actorColumn?: string;
  versionColumn: string; deletedColumn: string; createdAtColumn: string; updatedAtColumn: string; updatedByColumn: string;
  sort: number; version: number; enabled: boolean; readPermission: string; restorePermission: string; purgePermission: string;
}
export interface RecycleConfigurationInput {
  name: string; tableName: string; titleColumn: string; searchColumn: string; scopeRequired: boolean;
  scopeColumn?: string; departmentColumn?: string; ancestorsColumn?: string; actorColumn?: string; versionColumn?: string;
  deletedColumn?: string; createdAtColumn?: string; updatedAtColumn?: string; updatedByColumn?: string; sort?: number; enabled?: boolean; version?: number;
}
export const listRecycleConfigurations = () => api<RecycleConfiguration[]>(root);
export const recycleTableOptions = () => api<RecycleTableOption[]>(`${root}/tables`);
export const createRecycleConfiguration = (body: RecycleConfigurationInput) => api<RecycleConfiguration>(root, { method: 'POST', body });
export const updateRecycleConfiguration = (id: string, body: RecycleConfigurationInput) => api<RecycleConfiguration>(`${root}/${id}`, { method: 'PUT', body });
export const deleteRecycleConfiguration = (id: string, version: number) => api<void>(`${root}/${id}?version=${version}`, { method: 'DELETE' });
