import type {
  Dictionary, DictionaryItem, CreateDictionary, UpdateDictionary,
  CreateDictionaryItem, UpdateDictionaryItem, Page,
} from '@/types/api';
import { api } from '@/utils/request';

const root = '/api/admin/platform/dictionaries';
const dictionaryPath = (id: string) => `${root}/${encodeURIComponent(id)}`;
const itemsPath = (basicId: string) => `${dictionaryPath(basicId)}/items`;

export const listDictionaries = (page: number, size: number, name = '') =>
  api<Page<Dictionary>>(`${root}?page=${page}&size=${size}&name=${encodeURIComponent(name)}`);
export const dictionaryDetail = (id: string) => api<Dictionary>(dictionaryPath(id));
export const createDictionary = (body: CreateDictionary) =>
  api<Dictionary>(root, { method: 'POST', body });
export const updateDictionary = (id: string, body: UpdateDictionary) =>
  api<Dictionary>(dictionaryPath(id), { method: 'PUT', body });
export const deleteDictionary = (id: string, version: number) =>
  api<void>(`${dictionaryPath(id)}?version=${version}`, { method: 'DELETE' });

export const listDictionaryItems = (basicId: string, page: number, size: number, paramValue = '') =>
  api<Page<DictionaryItem>>(`${itemsPath(basicId)}?page=${page}&size=${size}&paramValue=${encodeURIComponent(paramValue)}`);
export const dictionaryItemDetail = (basicId: string, id: string) =>
  api<DictionaryItem>(`${itemsPath(basicId)}/${encodeURIComponent(id)}`);
export const createDictionaryItem = (basicId: string, body: CreateDictionaryItem) =>
  api<DictionaryItem>(itemsPath(basicId), { method: 'POST', body });
export const updateDictionaryItem = (basicId: string, id: string, body: UpdateDictionaryItem) =>
  api<DictionaryItem>(`${itemsPath(basicId)}/${encodeURIComponent(id)}`, { method: 'PUT', body });
export const deleteDictionaryItem = (basicId: string, id: string, version: number) =>
  api<void>(`${itemsPath(basicId)}/${encodeURIComponent(id)}?version=${version}`, { method: 'DELETE' });
