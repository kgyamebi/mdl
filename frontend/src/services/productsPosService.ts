import { apiRequest } from './apiClient';
import type { PageResponse } from '../types/api';

export type StockState = 'IN' | 'LOW' | 'OUT' | 'UNKNOWN';

export interface PosProductHit {
  id: number;
  sku: string;
  name: string;
  brand: string | null;
  categoryId: number | null;
  categoryName: string | null;
  unitOfMeasure: string;
  sellingPrice: number;
  currencyCode: string;
  reorderLevel: number | null;
  stockAvailable: number | null;
  stockState: StockState;
  favorite: boolean;
  saleCount90d: number;
}

export interface PosQuickAccess {
  recent: PosProductHit[];
  frequent: PosProductHit[];
  favorites: PosProductHit[];
}

export function posSearchProducts(params: {
  q?: string;
  categoryId?: number;
  locationId?: number;
  page?: number;
  size?: number;
}): Promise<PageResponse<PosProductHit>> {
  const query = new URLSearchParams();
  if (params.q) {
    query.set('q', params.q);
  }
  if (params.categoryId != null) {
    query.set('categoryId', String(params.categoryId));
  }
  if (params.locationId != null) {
    query.set('locationId', String(params.locationId));
  }
  query.set('page', String(params.page ?? 0));
  query.set('size', String(params.size ?? 40));
  return apiRequest<PageResponse<PosProductHit>>(`/api/pos/products/search?${query}`);
}

export function fetchPosQuickAccess(locationId?: number): Promise<PosQuickAccess> {
  const query = new URLSearchParams();
  if (locationId != null) {
    query.set('locationId', String(locationId));
  }
  const suffix = query.toString() ? `?${query}` : '';
  return apiRequest<PosQuickAccess>(`/api/pos/products/quick-access${suffix}`);
}

export function recordPosProductSelection(productId: number): Promise<void> {
  return apiRequest<void>(`/api/pos/products/${productId}/select`, { method: 'POST' });
}

export function togglePosProductFavorite(productId: number): Promise<{ favorite: boolean }> {
  return apiRequest<{ favorite: boolean }>(`/api/pos/products/${productId}/favorite`, {
    method: 'POST',
  });
}

export function recordPosSearchEvent(payload: {
  queryText: string;
  resultCount?: number;
  latencyMs?: number;
  selectedProductId?: number;
  selectionLatencyMs?: number;
  shopId?: number;
  failed?: boolean;
}): Promise<void> {
  return apiRequest<void>('/api/pos/products/search-events', {
    method: 'POST',
    body: payload,
  }).catch(() => undefined);
}
