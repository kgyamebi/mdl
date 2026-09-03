import { apiRequest } from './apiClient';
import type { OwnerAttentionReport } from '../types/api';

export function fetchAttentionDashboard(): Promise<OwnerAttentionReport> {
  return apiRequest<OwnerAttentionReport>('/api/alerts/attention');
}
