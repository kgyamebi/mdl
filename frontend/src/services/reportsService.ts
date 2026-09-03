import {
  clearSession,
  getAccessToken,
  getRefreshToken,
  saveSession,
} from '../auth/authStorage';
import type { ApiResponse, LoginResponse, PageResponse, ReportExport } from '../types/api';
import { apiRequest } from './apiClient';

const API_BASE = import.meta.env.VITE_API_BASE_URL ?? '';

function parseFileName(contentDisposition: string | null, fallback: string): string {
  if (!contentDisposition) {
    return fallback;
  }

  const match = /filename="([^"]+)"/i.exec(contentDisposition);
  return match?.[1] ?? fallback;
}

async function refreshAccessToken(): Promise<string | null> {
  const refreshToken = getRefreshToken();
  if (!refreshToken) {
    return null;
  }

  try {
    const response = await fetch(`${API_BASE}/api/auth/refresh`, {
      method: 'POST',
      headers: {
        Accept: 'application/json',
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ refreshToken }),
    });

    if (!response.ok) {
      return null;
    }

    const body: ApiResponse<LoginResponse> = await response.json();
    if (!body.success) {
      return null;
    }

    saveSession(body.data.accessToken, body.data.refreshToken, body.data.user);
    return body.data.accessToken;
  } catch {
    return null;
  }
}

async function downloadCsv(path: string, fallbackFileName: string): Promise<void> {
  const execute = (token?: string) =>
    fetch(`${API_BASE}${path}`, {
      headers: {
        Accept: 'text/csv, application/json',
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
    });

  let token = getAccessToken() ?? undefined;
  let response = await execute(token);

  if (response.status === 401) {
    const newToken = await refreshAccessToken();
    if (!newToken) {
      clearSession();
      throw new Error('Session expired. Please sign in again.');
    }
    token = newToken;
    response = await execute(token);
  }

  if (!response.ok) {
    try {
      const body: ApiResponse<unknown> = await response.json();
      throw new Error(body.message || `Export failed: ${response.status}`);
    } catch (err) {
      if (err instanceof Error && err.message !== `Export failed: ${response.status}`) {
        throw err;
      }
      throw new Error(`Export failed: ${response.status}`);
    }
  }

  const blob = await response.blob();
  const fileName = parseFileName(response.headers.get('Content-Disposition'), fallbackFileName);
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = fileName;
  link.click();
  URL.revokeObjectURL(url);
}

function appendInstantParam(query: URLSearchParams, key: string, value: string) {
  if (!value) {
    return;
  }
  const instant = new Date(value).toISOString();
  query.set(key, instant);
}

export function exportSalesSummaryCsv(params: {
  shopId?: number;
  from?: string;
  to?: string;
}): Promise<void> {
  const query = new URLSearchParams();
  if (params.shopId) {
    query.set('shopId', String(params.shopId));
  }
  appendInstantParam(query, 'from', params.from ?? '');
  appendInstantParam(query, 'to', params.to ?? '');

  const suffix = query.size > 0 ? `?${query}` : '';
  return downloadCsv(`/api/reports/sales-summary/export${suffix}`, 'sales-summary.csv');
}

export function exportInventoryBalancesCsv(params: {
  locationId?: number;
  lowStockOnly?: boolean;
}): Promise<void> {
  const query = new URLSearchParams();
  if (params.locationId) {
    query.set('locationId', String(params.locationId));
  }
  if (params.lowStockOnly) {
    query.set('lowStockOnly', 'true');
  }

  const suffix = query.size > 0 ? `?${query}` : '';
  return downloadCsv(`/api/reports/inventory-balances/export${suffix}`, 'inventory-balances.csv');
}

export function exportLowStockCsv(params: { locationId?: number }): Promise<void> {
  const query = new URLSearchParams();
  if (params.locationId) {
    query.set('locationId', String(params.locationId));
  }

  const suffix = query.size > 0 ? `?${query}` : '';
  return downloadCsv(`/api/reports/low-stock/export${suffix}`, 'low-stock.csv');
}

export function fetchReportExports(params: {
  reportType?: string;
  page?: number;
  size?: number;
}): Promise<PageResponse<ReportExport>> {
  const query = new URLSearchParams();
  if (params.reportType) {
    query.set('reportType', params.reportType);
  }
  query.set('page', String(params.page ?? 0));
  query.set('size', String(params.size ?? 20));

  return apiRequest<PageResponse<ReportExport>>(`/api/reports/exports?${query}`);
}

export function fetchReportExport(id: number): Promise<ReportExport> {
  return apiRequest<ReportExport>(`/api/reports/exports/${id}`);
}
