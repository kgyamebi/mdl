import { apiRequest } from './apiClient';
import type { PageResponse, Stocktake } from '../types/api';

export function fetchStocktakes(status = '', page = 0, size = 20): Promise<PageResponse<Stocktake>> {
  const query = new URLSearchParams({ page: String(page), size: String(size) });
  if (status) {
    query.set('status', status);
  }
  return apiRequest<PageResponse<Stocktake>>(`/api/inventory/stocktakes?${query}`);
}

export function fetchStocktake(id: number): Promise<Stocktake> {
  return apiRequest<Stocktake>(`/api/inventory/stocktakes/${id}`);
}

export function createStocktake(payload: {
  locationId: number;
  notes?: string;
  preloadBalances?: boolean;
}): Promise<Stocktake> {
  return apiRequest<Stocktake>('/api/inventory/stocktakes', { method: 'POST', body: payload });
}

export function upsertStocktakeLine(
  stocktakeId: number,
  payload: { productId: number; countedQuantity: number; notes?: string },
): Promise<Stocktake> {
  return apiRequest<Stocktake>(`/api/inventory/stocktakes/${stocktakeId}/lines`, {
    method: 'POST',
    body: payload,
  });
}

export function submitStocktake(stocktakeId: number): Promise<Stocktake> {
  return apiRequest<Stocktake>(`/api/inventory/stocktakes/${stocktakeId}/submit`, { method: 'POST' });
}

export function cancelStocktake(stocktakeId: number, reason: string): Promise<Stocktake> {
  return apiRequest<Stocktake>(`/api/inventory/stocktakes/${stocktakeId}/cancel`, {
    method: 'POST',
    body: { reason },
  });
}
