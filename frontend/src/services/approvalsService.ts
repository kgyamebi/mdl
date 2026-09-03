import { apiRequest } from './apiClient';
import type { ApprovalEntityType, ApprovalInboxResponse } from '../types/api';

export function fetchApprovalInbox(page = 0, size = 20): Promise<ApprovalInboxResponse> {
  const query = new URLSearchParams({ page: String(page), size: String(size) });
  return apiRequest<ApprovalInboxResponse>(`/api/approvals/inbox?${query}`);
}

export function canRejectEntityType(entityType: ApprovalEntityType): boolean {
  return entityType === 'INVENTORY_ADJUSTMENT' || entityType === 'STOCK_TRANSFER';
}

export function approveInboxItem(entityType: ApprovalEntityType, entityId: number): Promise<void> {
  switch (entityType) {
    case 'INVENTORY_ADJUSTMENT':
      return apiRequest(`/api/inventory/adjustment-requests/${entityId}/approve`, {
        method: 'POST',
        body: {},
      });
    case 'STOCK_TRANSFER':
      return apiRequest(`/api/stock-transfers/${entityId}/approve`, { method: 'POST' });
    case 'IMPORT_ORDER':
      return apiRequest(`/api/imports/${entityId}/approve`, { method: 'POST' });
    case 'STOCKTAKE':
      return apiRequest(`/api/inventory/stocktakes/${entityId}/approve`, {
        method: 'POST',
        body: {},
      });
    default:
      return Promise.reject(new Error(`Unsupported entity type: ${entityType}`));
  }
}

export function rejectInboxItem(
  entityType: ApprovalEntityType,
  entityId: number,
  notes: string,
): Promise<void> {
  switch (entityType) {
    case 'INVENTORY_ADJUSTMENT':
      return apiRequest(`/api/inventory/adjustment-requests/${entityId}/reject`, {
        method: 'POST',
        body: { reviewNotes: notes || null },
      });
    case 'STOCK_TRANSFER':
      return apiRequest(`/api/stock-transfers/${entityId}/reject`, {
        method: 'POST',
        body: { reason: notes },
      });
    default:
      return Promise.reject(new Error(`Reject is not supported for ${entityType}`));
  }
}

export function fetchApprovalRules(): Promise<import('../types/api').ApprovalRule[]> {
  return apiRequest<import('../types/api').ApprovalRule[]>('/api/approvals/rules');
}

export function fetchApprovalRule(id: number): Promise<import('../types/api').ApprovalRule> {
  return apiRequest<import('../types/api').ApprovalRule>(`/api/approvals/rules/${id}`);
}

export function updateApprovalRule(
  id: number,
  payload: {
    name?: string;
    description?: string;
    requiredPermission?: string;
    minAbsQuantity?: number | null;
    enabled?: boolean;
    priority?: number;
  },
): Promise<import('../types/api').ApprovalRule> {
  return apiRequest<import('../types/api').ApprovalRule>(`/api/approvals/rules/${id}`, {
    method: 'PATCH',
    body: payload,
  });
}
