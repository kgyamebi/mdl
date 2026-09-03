import { apiRequest } from './apiClient';
import type { InventoryBalance, InventorySummary, PageResponse } from '../types/api';

export function fetchInventorySummary(): Promise<InventorySummary> {
  return apiRequest<InventorySummary>('/api/inventory/summary');
}

export function fetchInventoryBalances(params: {
  locationId?: number;
  productId?: number;
  search?: string;
  lowStockOnly?: boolean;
  page?: number;
  size?: number;
}): Promise<PageResponse<InventoryBalance>> {
  const query = new URLSearchParams();
  if (params.locationId != null) {
    query.set('locationId', String(params.locationId));
  }
  if (params.productId != null) {
    query.set('productId', String(params.productId));
  }
  if (params.search) {
    query.set('search', params.search);
  }
  if (params.lowStockOnly) {
    query.set('lowStockOnly', 'true');
  }
  query.set('page', String(params.page ?? 0));
  query.set('size', String(params.size ?? 20));

  return apiRequest<PageResponse<InventoryBalance>>(`/api/inventory/balances?${query}`);
}
