import { apiRequest } from './apiClient';
import type { PageResponse, Product } from '../types/api';

export function fetchProducts(params: {
  search?: string;
  page?: number;
  size?: number;
}): Promise<PageResponse<Product>> {
  const query = new URLSearchParams();
  if (params.search) {
    query.set('search', params.search);
  }
  query.set('page', String(params.page ?? 0));
  query.set('size', String(params.size ?? 20));

  return apiRequest<PageResponse<Product>>(`/api/products?${query}`);
}

export function lookupProductByBarcode(barcode: string): Promise<Product> {
  const query = new URLSearchParams({ barcode: barcode.trim() });
  return apiRequest<Product>(`/api/products/lookup?${query}`);
}
