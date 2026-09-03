import { apiRequest } from './apiClient';
import type { PageResponse, PaymentMethod, Sale } from '../types/api';

export function fetchSales(params: {
  status?: string;
  page?: number;
  size?: number;
}): Promise<PageResponse<Sale>> {
  const query = new URLSearchParams();
  if (params.status) {
    query.set('status', params.status);
  }
  query.set('page', String(params.page ?? 0));
  query.set('size', String(params.size ?? 20));

  return apiRequest<PageResponse<Sale>>(`/api/sales?${query}`);
}

export function fetchSale(id: number): Promise<Sale> {
  return apiRequest<Sale>(`/api/sales/${id}`);
}

export function createSale(payload: {
  shopId: number;
  customerName?: string;
  notes?: string;
  items: { productId: number; quantity: number; unitPrice?: number }[];
  payments: { paymentMethod: PaymentMethod; amount: number; reference?: string }[];
}): Promise<Sale> {
  return apiRequest<Sale>('/api/sales', {
    method: 'POST',
    body: payload,
  });
}

export function cancelSale(id: number, reason: string): Promise<Sale> {
  return apiRequest<Sale>(`/api/sales/${id}/cancel`, {
    method: 'POST',
    body: { reason },
  });
}

export function refundSale(id: number, reason: string): Promise<Sale> {
  return apiRequest<Sale>(`/api/sales/${id}/refund`, {
    method: 'POST',
    body: { reason },
  });
}
