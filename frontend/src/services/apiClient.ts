import {
  clearSession,
  getAccessToken,
  saveSession,
} from '../auth/authStorage';
import type { ApiResponse, AuthUser, LoginResponse } from '../types/api';

const API_BASE = import.meta.env.VITE_API_BASE_URL ?? '';

type RequestOptions = Omit<RequestInit, 'body'> & {
  body?: unknown;
  auth?: boolean;
};

let refreshPromise: Promise<string | null> | null = null;
let onSessionExpired: (() => void) | null = null;

export function setSessionExpiredHandler(handler: () => void): void {
  onSessionExpired = handler;
}

async function parseResponse<T>(response: Response): Promise<T> {
  const body: ApiResponse<T> = await response.json();
  if (!response.ok || !body.success) {
    throw new Error(body.message || `Request failed: ${response.status}`);
  }
  return body.data;
}

async function refreshAccessToken(): Promise<string | null> {
  if (!refreshPromise) {
    refreshPromise = (async () => {
      try {
        const response = await fetch(`${API_BASE}/api/auth/refresh`, {
          method: 'POST',
          headers: {
            Accept: 'application/json',
            'Content-Type': 'application/json',
          },
          credentials: 'include',
        });

        if (!response.ok) {
          return null;
        }

        const data = await parseResponse<LoginResponse>(response);
        if (data.accessToken && data.user) {
          saveSession(data.accessToken, null, data.user);
          return data.accessToken;
        }
        return null;
      } catch {
        return null;
      } finally {
        refreshPromise = null;
      }
    })();
  }

  return refreshPromise;
}

export async function apiRequest<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const { body, auth = true, headers, ...rest } = options;

  const requestHeaders: Record<string, string> = {
    Accept: 'application/json',
    ...(headers as Record<string, string>),
  };

  if (body !== undefined) {
    requestHeaders['Content-Type'] = 'application/json';
  }

  if (auth) {
    const token = getAccessToken();
    if (token) {
      requestHeaders.Authorization = `Bearer ${token}`;
    }
  }

  const execute = (tokenOverride?: string) =>
    fetch(`${API_BASE}${path}`, {
      ...rest,
      credentials: 'include',
      headers: {
        ...requestHeaders,
        ...(tokenOverride ? { Authorization: `Bearer ${tokenOverride}` } : {}),
      },
      body: body !== undefined ? JSON.stringify(body) : undefined,
    });

  let response = await execute();

  if (response.status === 401 && auth) {
    const newToken = await refreshAccessToken();
    if (newToken) {
      response = await execute(newToken);
    } else {
      clearSession();
      onSessionExpired?.();
      throw new Error('Session expired. Please sign in again.');
    }
  }

  return parseResponse<T>(response);
}

export async function loginRequest(login: string, password: string): Promise<LoginResponse> {
  const response = await fetch(`${API_BASE}/api/auth/login`, {
    method: 'POST',
    headers: {
      Accept: 'application/json',
      'Content-Type': 'application/json',
    },
    credentials: 'include',
    body: JSON.stringify({ login, password }),
  });
  return parseResponse<LoginResponse>(response);
}

export async function mfaChallengeRequest(mfaToken: string, code: string): Promise<LoginResponse> {
  const response = await fetch(`${API_BASE}/api/auth/mfa/challenge`, {
    method: 'POST',
    headers: {
      Accept: 'application/json',
      'Content-Type': 'application/json',
    },
    credentials: 'include',
    body: JSON.stringify({ mfaToken, code }),
  });
  return parseResponse<LoginResponse>(response);
}

export async function logoutRequest(): Promise<void> {
  try {
    await fetch(`${API_BASE}/api/auth/logout`, {
      method: 'POST',
      headers: {
        Accept: 'application/json',
        'Content-Type': 'application/json',
      },
      credentials: 'include',
    });
  } catch {
    // Clear local session even if revoke fails
  }
  clearSession();
}

export async function fetchCurrentUser(): Promise<AuthUser> {
  return apiRequest<AuthUser>('/api/auth/me');
}

export async function bootstrapSession(): Promise<LoginResponse | null> {
  const response = await fetch(`${API_BASE}/api/auth/refresh`, {
    method: 'POST',
    headers: {
      Accept: 'application/json',
      'Content-Type': 'application/json',
    },
    credentials: 'include',
  });
  if (!response.ok) {
    return null;
  }
  return parseResponse<LoginResponse>(response);
}

export function getHealthStatus() {
  return apiRequest<import('../types/api').HealthStatus>('/api/health', { auth: false });
}

export async function mfaSetupRequest(): Promise<import('../types/api').MfaSetupResponse> {
  return apiRequest<import('../types/api').MfaSetupResponse>('/api/auth/mfa/setup', { method: 'POST' });
}

export async function mfaConfirmRequest(code: string): Promise<void> {
  await apiRequest<null>('/api/auth/mfa/confirm', {
    method: 'POST',
    body: { code },
  });
}
