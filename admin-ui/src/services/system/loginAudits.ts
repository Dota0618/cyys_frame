import { api } from '@/utils/request';
import type { Page, LoginAuditOptions, LoginAuditRecord } from '@/types/api';

export const loginAuditOptions = () => api<LoginAuditOptions>('/api/admin/login-audits/options');

export const listLoginAudits = (
  page: number,
  size: number,
  loginName?: string,
  outcome?: string,
) => {
  const query = new URLSearchParams({ page: String(page), size: String(size) });
  if (loginName) query.set('loginName', loginName);
  if (outcome) query.set('outcome', outcome);
  return api<Page<LoginAuditRecord>>(`/api/admin/login-audits?${query.toString()}`);
};
