import { api } from '@/utils/request';
import type { AuditRecord, Page } from '@/types/api';

export function listAudits(page: number, size: number, resource?: string, operation?: string) {
  const query = new URLSearchParams({ page: String(page), size: String(size) });
  if (resource) query.set('resource', resource);
  if (operation) query.set('operation', operation);
  return api<Page<AuditRecord>>(`/api/admin/audits?${query.toString()}`);
}
