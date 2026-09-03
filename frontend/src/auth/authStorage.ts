import type { AuthUser, LoginResponse } from '../types/api';

const USER_KEY = 'mdl_user';

let memoryAccessToken: string | null = null;

export function getAccessToken(): string | null {
  return memoryAccessToken;
}

export function getRefreshToken(): string | null {
  return null;
}

export function getStoredUser(): AuthUser | null {
  const raw = sessionStorage.getItem(USER_KEY);
  if (!raw) {
    return null;
  }
  try {
    return JSON.parse(raw) as AuthUser;
  } catch {
    return null;
  }
}

export function saveSession(accessToken: string | null, _refreshToken: string | null, user: AuthUser): void {
  memoryAccessToken = accessToken;
  sessionStorage.setItem(USER_KEY, JSON.stringify(user));
}

export function clearSession(): void {
  memoryAccessToken = null;
  sessionStorage.removeItem(USER_KEY);
}

export type { LoginResponse };
