import { apiRequest } from './apiClient';
import type { AuditLog, PageResponse } from '../types/api';

export function fetchAuditLogs(params: {
  userId?: number;
  module?: string;
  action?: string;
  entityType?: string;
  entityId?: number;
  from?: string;
  to?: string;
  page?: number;
  size?: number;
}): Promise<PageResponse<AuditLog>> {
  const query = new URLSearchParams();
  if (params.userId != null) {
    query.set('userId', String(params.userId));
  }
  if (params.module?.trim()) {
    query.set('module', params.module.trim());
  }
  if (params.action?.trim()) {
    query.set('action', params.action.trim());
  }
  if (params.entityType?.trim()) {
    query.set('entityType', params.entityType.trim());
  }
  if (params.entityId != null) {
    query.set('entityId', String(params.entityId));
  }
  if (params.from) {
    query.set('from', params.from);
  }
  if (params.to) {
    query.set('to', params.to);
  }
  query.set('page', String(params.page ?? 0));
  query.set('size', String(params.size ?? 20));
  return apiRequest<PageResponse<AuditLog>>(`/api/audit-logs?${query}`);
}
