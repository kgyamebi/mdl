import { apiRequest } from './apiClient';
import type { PageResponse, SaleReturn } from '../types/api';

export function fetchSaleReturns(page = 0, size = 20): Promise<PageResponse<SaleReturn>> {
  return apiRequest<PageResponse<SaleReturn>>(`/api/sale-returns?page=${page}&size=${size}`);
}

export function fetchSaleReturn(id: number): Promise<SaleReturn> {
  return apiRequest<SaleReturn>(`/api/sale-returns/${id}`);
}

export interface CreateSaleReturnPayload {
  reason: string;
  notes?: string;
  items: Array<{ saleItemId: number; quantity: number }>;
  refunds: Array<{ paymentMethod: string; amount: number; reference?: string }>;
}

export function createSaleReturn(saleId: number, payload: CreateSaleReturnPayload): Promise<SaleReturn> {
  return apiRequest<SaleReturn>(`/api/sales/${saleId}/returns`, {
    method: 'POST',
    body: payload,
  });
}
