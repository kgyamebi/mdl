import { apiRequest } from './apiClient';
import type { NotificationItem, PageResponse, UnreadNotificationCount } from '../types/api';

export function fetchNotifications(params: {
  status?: string;
  category?: string;
  page?: number;
  size?: number;
}): Promise<PageResponse<NotificationItem>> {
  const query = new URLSearchParams();
  if (params.status) {
    query.set('status', params.status);
  }
  if (params.category) {
    query.set('category', params.category);
  }
  query.set('page', String(params.page ?? 0));
  query.set('size', String(params.size ?? 20));

  return apiRequest<PageResponse<NotificationItem>>(`/api/notifications?${query}`);
}

export function fetchUnreadNotificationCount(): Promise<UnreadNotificationCount> {
  return apiRequest<UnreadNotificationCount>('/api/notifications/unread-count');
}

export function markNotificationRead(id: number): Promise<NotificationItem> {
  return apiRequest<NotificationItem>(`/api/notifications/${id}/read`, { method: 'POST' });
}

export function dismissNotification(id: number): Promise<NotificationItem> {
  return apiRequest<NotificationItem>(`/api/notifications/${id}/dismiss`, { method: 'POST' });
}

export function markAllNotificationsRead(): Promise<number> {
  return apiRequest<number>('/api/notifications/mark-all-read', { method: 'POST' });
}
