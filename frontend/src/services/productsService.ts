import { apiRequest } from './apiClient';
import type { PageResponse, Product, ProductCategory } from '../types/api';

export function fetchProducts(params: {
  search?: string;
  categoryId?: number;
  status?: string;
  page?: number;
  size?: number;
}): Promise<PageResponse<Product>> {
  const query = new URLSearchParams();
  if (params.search) {
    query.set('search', params.search);
  }
  if (params.categoryId != null) {
    query.set('categoryId', String(params.categoryId));
  }
  if (params.status) {
    query.set('status', params.status);
  }
  query.set('page', String(params.page ?? 0));
  query.set('size', String(params.size ?? 20));

  return apiRequest<PageResponse<Product>>(`/api/products?${query}`);
}

export function fetchProduct(id: number): Promise<Product> {
  return apiRequest<Product>(`/api/products/${id}`);
}

export function lookupProductByBarcode(barcode: string): Promise<Product> {
  const query = new URLSearchParams({ barcode: barcode.trim() });
  return apiRequest<Product>(`/api/products/lookup?${query}`);
}

export function fetchProductCategories(activeOnly = true): Promise<ProductCategory[]> {
  const query = new URLSearchParams({ activeOnly: String(activeOnly) });
  return apiRequest<ProductCategory[]>(`/api/product-categories?${query}`);
}

export interface CreateProductPayload {
  categoryId?: number | null;
  sku: string;
  name: string;
  description?: string;
  brand?: string;
  unitOfMeasure: string;
  costPrice?: number;
  sellingPrice: number;
  taxInclusive?: boolean;
  trackInventory?: boolean;
  reorderLevel?: number;
  status?: string;
}

export function createProduct(payload: CreateProductPayload): Promise<Product> {
  return apiRequest<Product>('/api/products', { method: 'POST', body: payload });
}

export interface UpdateProductPayload {
  categoryId?: number | null;
  name: string;
  description?: string;
  brand?: string;
  unitOfMeasure: string;
  costPrice?: number;
  sellingPrice: number;
  taxInclusive?: boolean;
  trackInventory?: boolean;
  reorderLevel?: number;
  status: string;
}

export function updateProduct(id: number, payload: UpdateProductPayload): Promise<Product> {
  return apiRequest<Product>(`/api/products/${id}`, { method: 'PUT', body: payload });
}

export function addProductBarcode(
  productId: number,
  payload: { barcode: string; barcodeType: string; primary?: boolean },
): Promise<Product> {
  return apiRequest<Product>(`/api/products/${productId}/barcodes`, { method: 'POST', body: payload });
}

export function removeProductBarcode(productId: number, barcodeId: number): Promise<void> {
  return apiRequest<void>(`/api/products/${productId}/barcodes/${barcodeId}`, { method: 'DELETE' });
}
