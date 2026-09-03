import { apiRequest } from './apiClient';
import type { BusinessProfile, CurrencyOption } from '../types/api';

export function fetchBusiness(): Promise<BusinessProfile> {
  return apiRequest<BusinessProfile>('/api/business');
}

export function updateBusiness(payload: {
  name: string;
  legalName?: string;
  currencyCode: string;
  timezone: string;
}): Promise<BusinessProfile> {
  return apiRequest<BusinessProfile>('/api/business', { method: 'PUT', body: payload });
}

export function fetchCurrencies(): Promise<CurrencyOption[]> {
  return apiRequest<CurrencyOption[]>('/api/business/currencies');
}
