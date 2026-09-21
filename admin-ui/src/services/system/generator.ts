import { api, apiBlob } from '@/utils/request';

export type GeneratorFieldType = 'TEXT' | 'TEXTAREA' | 'INTEGER' | 'DECIMAL' | 'BOOLEAN' | 'DATE' | 'DATETIME' | 'DICTIONARY';
export interface GeneratorField {
  column: string; label: string; type: GeneratorFieldType; required: boolean; inList: boolean; searchable: boolean;
  length?: number | null; dictionary?: string | null;
}
export interface GeneratorCommand {
  table: string; feature: string; className: string; title: string; layout: 'LIST' | 'TREE'; existingTable: boolean; fields: GeneratorField[];
}
export interface GeneratorTable { name: string; comment: string }
export interface GeneratorColumn { name: string; dataType: string; columnType: string; length?: number; nullable: boolean; comment: string }
export interface GeneratedSource { path: string; content: string }
export interface GeneratorPreview { templateVersion: string; sha256: string; files: GeneratedSource[] }
const base = '/api/admin/platform/generator';
export const tables = () => api<GeneratorTable[]>(`${base}/tables`);
export const columns = (table: string) => api<GeneratorColumn[]>(`${base}/tables/${encodeURIComponent(table)}`);
export const preview = (command: GeneratorCommand) => api<GeneratorPreview>(`${base}/preview`, { method: 'POST', body: command });
export const download = (command: GeneratorCommand) => apiBlob(`${base}/download`, { method: 'POST', body: command });
