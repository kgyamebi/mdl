import {
  clearSession,
  getAccessToken,
  saveSession,
} from '../auth/authStorage';
import { resolveApiBase } from '../config/apiBase';
import type { ApiResponse, AuthUser, LoginResponse } from '../types/api';

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
  const contentType = response.headers.get('content-type') ?? '';
  let body: ApiResponse<T>;

  try {
    if (!contentType.includes('application/json')) {
      throw new Error('non_json');
    }
    body = await response.json();
  } catch {
    if (!response.ok) {
      throw new Error(
        response.status >= 500
          ? 'Server is unavailable. Make sure the backend is running on this computer.'
          : 'Could not reach the server. Check your network connection and try again.',
      );
    }
    throw new Error('Unexpected server response. Please try again.');
  }

  if (!response.ok || !body.success) {
    throw new Error(body.message || `Request failed: ${response.status}`);
  }
  return body.data;
}

function toFriendlyError(error: unknown): string {
  if (!(error instanceof Error)) {
    return 'Sign in failed. Please try again.';
  }

  const message = error.message.trim();
  if (
    message === 'The string did not match the expected pattern.' ||
    message === 'JSON Parse error: Unexpected EOF' ||
    message.startsWith('JSON Parse error')
  ) {
    return 'Could not reach the server. Make sure the backend is running and you are on the same Wi‑Fi network.';
  }

  if (message === 'Load failed' || message === 'Failed to fetch' || message === 'NetworkError when attempting to fetch resource.') {
    return 'Cannot reach the server. On your PC, make sure the backend (port 8080) and frontend (port 5173) are both running, then refresh this page.';
  }

  return message || 'Sign in failed. Please try again.';
}

async function refreshAccessToken(): Promise<string | null> {
  if (!refreshPromise) {
    refreshPromise = (async () => {
      try {
        const response = await fetch(`${resolveApiBase()}/api/auth/refresh`, {
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
    fetch(`${resolveApiBase()}${path}`, {
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
  try {
    const response = await fetch(`${resolveApiBase()}/api/auth/login`, {
      method: 'POST',
      headers: {
        Accept: 'application/json',
        'Content-Type': 'application/json',
      },
      credentials: 'include',
      body: JSON.stringify({ login, password }),
    });
    return await parseResponse<LoginResponse>(response);
  } catch (error) {
    throw new Error(toFriendlyError(error));
  }
}

export async function mfaChallengeRequest(mfaToken: string, code: string): Promise<LoginResponse> {
  const response = await fetch(`${resolveApiBase()}/api/auth/mfa/challenge`, {
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
    await fetch(`${resolveApiBase()}/api/auth/logout`, {
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
  const response = await fetch(`${resolveApiBase()}/api/auth/refresh`, {
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
