import { api } from '@/utils/request';
import type { AuditOptions, AuditRecord, Page } from '@/types/api';

export const auditOptions = () => api<AuditOptions>('/api/admin/audits/options');

export function listAudits(page: number, size: number, resource?: string, operation?: string, outcome?: string) {
  const query = new URLSearchParams({ page: String(page), size: String(size) });
  if (resource) query.set('resource', resource);
  if (operation) query.set('operation', operation);
  if (outcome) query.set('outcome', outcome);
  return api<Page<AuditRecord>>(`/api/admin/audits?${query.toString()}`);
}
