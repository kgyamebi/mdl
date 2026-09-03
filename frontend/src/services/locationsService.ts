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
