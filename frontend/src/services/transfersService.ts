import { apiRequest } from './apiClient';
import type { PageResponse, StockTransfer, TransferFormOptions } from '../types/api';

export function fetchTransferFormOptions(): Promise<TransferFormOptions> {
  return apiRequest<TransferFormOptions>('/api/stock-transfers/form-options');
}

export function fetchTransfers(params: {
  status?: string;
  page?: number;
  size?: number;
}): Promise<PageResponse<StockTransfer>> {
  const query = new URLSearchParams();
  if (params.status) {
    query.set('status', params.status);
  }
  query.set('page', String(params.page ?? 0));
  query.set('size', String(params.size ?? 20));

  return apiRequest<PageResponse<StockTransfer>>(`/api/stock-transfers?${query}`);
}

export function fetchTransfer(id: number): Promise<StockTransfer> {
  return apiRequest<StockTransfer>(`/api/stock-transfers/${id}`);
}

export function approveTransfer(id: number): Promise<StockTransfer> {
  return apiRequest<StockTransfer>(`/api/stock-transfers/${id}/approve`, { method: 'POST' });
}

export function rejectTransfer(id: number, reason: string): Promise<StockTransfer> {
  return apiRequest<StockTransfer>(`/api/stock-transfers/${id}/reject`, {
    method: 'POST',
    body: { reason },
  });
}

export function dispatchTransfer(id: number): Promise<StockTransfer> {
  return apiRequest<StockTransfer>(`/api/stock-transfers/${id}/dispatch`, { method: 'POST' });
}

export function receiveTransfer(
  id: number,
  items: { itemId: number; quantityReceived: number; notes?: string | null }[],
): Promise<StockTransfer> {
  return apiRequest<StockTransfer>(`/api/stock-transfers/${id}/receive`, {
    method: 'POST',
    body: { items },
  });
}

export function cancelTransfer(id: number): Promise<StockTransfer> {
  return apiRequest<StockTransfer>(`/api/stock-transfers/${id}/cancel`, { method: 'POST' });
}

export function createTransfer(payload: {
  fromWarehouseId: number;
  toWarehouseId: number;
  notes?: string;
  items: { productId: number; quantity: number; notes?: string }[];
}): Promise<StockTransfer> {
  return apiRequest<StockTransfer>('/api/stock-transfers', {
    method: 'POST',
    body: payload,
  });
}
