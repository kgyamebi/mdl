import { apiRequest } from './apiClient';
import type { LocationSummary, Shop, Warehouse } from '../types/api';

export function fetchWarehouses(type?: string): Promise<Warehouse[]> {
  const query = type ? `?type=${encodeURIComponent(type)}` : '';
  return apiRequest<Warehouse[]>(`/api/warehouses${query}`);
}

export function fetchLocations(): Promise<LocationSummary[]> {
  return apiRequest<LocationSummary[]>('/api/locations');
}

export function fetchShops(): Promise<Shop[]> {
  return apiRequest<Shop[]>('/api/shops');
}

export function fetchBusinessStructure(): Promise<import('../types/api').BusinessStructure> {
  return apiRequest<import('../types/api').BusinessStructure>('/api/business/structure');
}

export function fetchTransferRoutes(): Promise<import('../types/api').TransferRoute[]> {
  return apiRequest<import('../types/api').TransferRoute[]>('/api/transfer-routes');
}

export function createTransferRoute(payload: {
  fromWarehouseId: number;
  toWarehouseId: number;
  notes?: string;
}): Promise<import('../types/api').TransferRoute> {
  return apiRequest<import('../types/api').TransferRoute>('/api/transfer-routes', {
    method: 'POST',
    body: payload,
  });
}

export function updateTransferRoute(
  id: number,
  payload: { enabled: boolean; notes?: string },
): Promise<import('../types/api').TransferRoute> {
  return apiRequest<import('../types/api').TransferRoute>(`/api/transfer-routes/${id}`, {
    method: 'PUT',
    body: payload,
  });
}

export function createShop(payload: {
  name: string;
  code?: string;
  city?: string;
  country?: string;
}): Promise<Shop> {
  return apiRequest<Shop>('/api/shops', { method: 'POST', body: payload });
}

export function deactivateShop(id: number): Promise<Shop> {
  return apiRequest<Shop>(`/api/shops/${id}`, { method: 'DELETE' });
}

export function createMainWarehouse(payload: {
  name: string;
  code?: string;
  city?: string;
  country?: string;
  description?: string;
  restricted?: boolean;
  warehouseType?: 'MAIN' | 'SHOP';
}): Promise<Warehouse> {
  return apiRequest<Warehouse>('/api/warehouses', { method: 'POST', body: payload });
}

export function deactivateWarehouse(id: number): Promise<Warehouse> {
  return apiRequest<Warehouse>(`/api/warehouses/${id}`, { method: 'DELETE' });
}

export function deleteTransferRoute(id: number): Promise<void> {
  return apiRequest<void>(`/api/transfer-routes/${id}`, { method: 'DELETE' });
}
