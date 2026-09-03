import { apiRequest } from './apiClient';
import type { ImportOrder, PageResponse } from '../types/api';

export function fetchImports(params: {
  status?: string;
  page?: number;
  size?: number;
}): Promise<PageResponse<ImportOrder>> {
  const query = new URLSearchParams();
  if (params.status) {
    query.set('status', params.status);
  }
  query.set('page', String(params.page ?? 0));
  query.set('size', String(params.size ?? 20));

  return apiRequest<PageResponse<ImportOrder>>(`/api/imports?${query}`);
}

export function fetchImport(id: number): Promise<ImportOrder> {
  return apiRequest<ImportOrder>(`/api/imports/${id}`);
}

export function createImport(payload: {
  supplierName: string;
  supplierReference?: string;
  destinationLocationId: number;
  expectedArrivalDate?: string;
  notes?: string;
  items: { productId: number; expectedQuantity: number; unitCost?: number }[];
}): Promise<ImportOrder> {
  return apiRequest<ImportOrder>('/api/imports', {
    method: 'POST',
    body: payload,
  });
}

export function submitImport(id: number): Promise<ImportOrder> {
  return apiRequest<ImportOrder>(`/api/imports/${id}/submit`, { method: 'POST' });
}

export function approveImport(id: number): Promise<ImportOrder> {
  return apiRequest<ImportOrder>(`/api/imports/${id}/approve`, { method: 'POST' });
}

export function receiveImport(
  id: number,
  items: { itemId: number; quantityReceived: number; notes?: string }[],
): Promise<ImportOrder> {
  return apiRequest<ImportOrder>(`/api/imports/${id}/receive`, {
    method: 'POST',
    body: { items },
  });
}

export function verifyImport(id: number): Promise<ImportOrder> {
  return apiRequest<ImportOrder>(`/api/imports/${id}/verify`, { method: 'POST' });
}

export function cancelImport(id: number): Promise<ImportOrder> {
  return apiRequest<ImportOrder>(`/api/imports/${id}/cancel`, { method: 'POST' });
}
