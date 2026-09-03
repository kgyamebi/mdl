import { apiRequest } from './apiClient';
import type { ManagedUser, PageResponse, RoleOption } from '../types/api';

export function fetchUsers(search = '', page = 0, size = 20): Promise<PageResponse<ManagedUser>> {
  const query = new URLSearchParams({ page: String(page), size: String(size) });
  if (search.trim()) {
    query.set('search', search.trim());
  }
  return apiRequest<PageResponse<ManagedUser>>(`/api/users?${query}`);
}

export function fetchUser(id: number): Promise<ManagedUser> {
  return apiRequest<ManagedUser>(`/api/users/${id}`);
}

export function fetchRoles(): Promise<RoleOption[]> {
  return apiRequest<RoleOption[]>('/api/roles');
}

export interface CreateUserPayload {
  email: string;
  username: string;
  password: string;
  firstName: string;
  lastName: string;
  phone?: string;
  roleCodes?: string[];
  locationIds?: number[];
}

export function createUser(payload: CreateUserPayload): Promise<ManagedUser> {
  return apiRequest<ManagedUser>('/api/users', { method: 'POST', body: payload });
}

export function updateUser(
  id: number,
  payload: { firstName: string; lastName: string; phone?: string },
): Promise<ManagedUser> {
  return apiRequest<ManagedUser>(`/api/users/${id}`, { method: 'PUT', body: payload });
}

export function updateUserStatus(id: number, status: string): Promise<ManagedUser> {
  return apiRequest<ManagedUser>(`/api/users/${id}/status`, { method: 'PUT', body: { status } });
}

export function updateUserRoles(id: number, roleCodes: string[]): Promise<ManagedUser> {
  return apiRequest<ManagedUser>(`/api/users/${id}/roles`, { method: 'PUT', body: { roleCodes } });
}

export function updateUserLocations(
  id: number,
  locations: Array<{ locationId: number; accessLevel: string }>,
): Promise<ManagedUser> {
  return apiRequest<ManagedUser>(`/api/users/${id}/locations`, {
    method: 'PUT',
    body: { locations },
  });
}
