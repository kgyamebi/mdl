import {
  clearSession,
  getAccessToken,
  saveSession,
} from '../auth/authStorage';
import { buildApiUrl, getApiDebugInfo, resolveApiBase } from '../config/apiBase';
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

function logApiDebug(message: string, details?: Record<string, unknown>): void {
  if (import.meta.env.DEV) {
    console.debug('[MDL API]', message, details ?? '');
  }
}

async function parseResponse<T>(response: Response, requestUrl: string): Promise<T> {
  const contentType = response.headers.get('content-type') ?? '';
  let body: ApiResponse<T>;

  try {
    if (!contentType.includes('application/json')) {
      const preview = (await response.text()).slice(0, 200);
      logApiDebug('Non-JSON response', {
        url: requestUrl,
        status: response.status,
        contentType,
        preview,
      });
      throw new Error('non_json');
    }
    body = await response.json();
  } catch (error) {
    if (error instanceof Error && error.message !== 'non_json') {
      logApiDebug('JSON parse failed', {
        url: requestUrl,
        status: response.status,
        error: error.message,
      });
    }

    if (!response.ok) {
      throw new Error(
        response.status >= 500
          ? `Server error (${response.status}). Check that the backend is running on port 8080.`
          : `Could not reach the API (${response.status}). URL: ${requestUrl}`,
      );
    }
    throw new Error(`Unexpected server response from ${requestUrl}. Expected JSON.`);
  }

  if (!response.ok || !body.success) {
    logApiDebug('API error response', {
      url: requestUrl,
      status: response.status,
      message: body.message,
    });
    throw new Error(body.message || `Request failed: ${response.status}`);
  }

  return body.data;
}

function toFriendlyError(error: unknown, requestUrl?: string): string {
  if (!(error instanceof Error)) {
    return 'Sign in failed. Please try again.';
  }

  const message = error.message.trim();
  const urlHint = requestUrl ? ` (${requestUrl})` : '';

  if (message.startsWith('Invalid API URL')) {
    return `${message} Fix VITE_API_BASE_URL in your .env file.`;
  }

  if (
    message === 'The string did not match the expected pattern.' ||
    message === 'JSON Parse error: Unexpected EOF' ||
    message.startsWith('JSON Parse error')
  ) {
    const info = getApiDebugInfo();
    return (
      `Cannot reach the backend${urlHint}. ` +
      `Page: ${info.pageOrigin}, API mode: ${info.mode}, API base: ${info.apiBase}. ` +
      'On mobile, use http://YOUR-PC-IP:5173 and ensure backend listens on 0.0.0.0:8080.'
    );
  }

  if (
    message === 'Load failed' ||
    message === 'Failed to fetch' ||
    message === 'NetworkError when attempting to fetch resource.'
  ) {
    const info = getApiDebugInfo();
    return (
      `Network error — cannot reach the backend${urlHint}. ` +
      `Try opening ${info.sampleLoginUrl.replace('/api/auth/login', '/api/health')} in Safari first. ` +
      `API mode: ${info.mode}.`
    );
  }

  return message || 'Sign in failed. Please try again.';
}

async function refreshAccessToken(): Promise<string | null> {
  if (!refreshPromise) {
    refreshPromise = (async () => {
      const url = buildApiUrl('/api/auth/refresh');
      try {
        const response = await fetch(url, {
          method: 'POST',
          headers: {
            Accept: 'application/json',
            'Content-Type': 'application/json',
          },
          credentials: 'include',
        });

        if (!response.ok) {
          logApiDebug('Refresh failed', { url, status: response.status });
          return null;
        }

        const data = await parseResponse<LoginResponse>(response, url);
        if (data.accessToken && data.user) {
          saveSession(data.accessToken, null, data.user);
          return data.accessToken;
        }
        return null;
      } catch (error) {
        logApiDebug('Refresh network error', { url, error: String(error) });
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
  const requestUrl = buildApiUrl(path);

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
    fetch(requestUrl, {
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

  return parseResponse<T>(response, requestUrl);
}

export async function loginRequest(login: string, password: string): Promise<LoginResponse> {
  const requestUrl = buildApiUrl('/api/auth/login');
  logApiDebug('Login attempt', getApiDebugInfo());

  try {
    const response = await fetch(requestUrl, {
      method: 'POST',
      headers: {
        Accept: 'application/json',
        'Content-Type': 'application/json',
      },
      credentials: 'include',
      body: JSON.stringify({ login, password }),
    });

    logApiDebug('Login response', { url: requestUrl, status: response.status });
    return await parseResponse<LoginResponse>(response, requestUrl);
  } catch (error) {
    logApiDebug('Login failed', { url: requestUrl, error: String(error) });
    throw new Error(toFriendlyError(error, requestUrl));
  }
}

export async function mfaChallengeRequest(mfaToken: string, code: string): Promise<LoginResponse> {
  const requestUrl = buildApiUrl('/api/auth/mfa/challenge');
  const response = await fetch(requestUrl, {
    method: 'POST',
    headers: {
      Accept: 'application/json',
      'Content-Type': 'application/json',
    },
    credentials: 'include',
    body: JSON.stringify({ mfaToken, code }),
  });
  return parseResponse<LoginResponse>(response, requestUrl);
}

export async function logoutRequest(): Promise<void> {
  try {
    await fetch(buildApiUrl('/api/auth/logout'), {
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
  const requestUrl = buildApiUrl('/api/auth/refresh');
  try {
    const response = await fetch(requestUrl, {
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
    return parseResponse<LoginResponse>(response, requestUrl);
  } catch (error) {
    logApiDebug('Bootstrap refresh failed', { url: requestUrl, error: String(error) });
    return null;
  }
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

/** @deprecated use buildApiUrl from config/apiBase */
export { resolveApiBase };
