import { apiRequest } from './apiClient';
import type { BusinessOverviewReport } from '../types/api';

export function fetchBusinessOverview(): Promise<BusinessOverviewReport> {
  return apiRequest<BusinessOverviewReport>('/api/reports/business-overview');
}
