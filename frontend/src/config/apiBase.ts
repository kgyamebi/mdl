/**
 * Resolves the API base URL for the current runtime.
 *
 * Priority:
 * 1. VITE_API_BASE_URL or VITE_API_URL (explicit override)
 * 2. Dev + LAN host (e.g. phone at 192.168.x.x:5173) → same host on port 8080
 * 3. Dev + localhost → empty string (Vite proxy on :5173)
 * 4. Production build → empty string (nginx/Caddy proxy /api)
 */

const LOCAL_HOSTS = new Set(['localhost', '127.0.0.1', '[::1]']);

function isLocalHost(hostname: string): boolean {
  return LOCAL_HOSTS.has(hostname.toLowerCase());
}

function normalizeBaseUrl(value: string): string {
  const trimmed = value.trim();
  if (!trimmed) {
    return '';
  }

  let url = trimmed;
  if (!/^https?:\/\//i.test(url)) {
    url = `http://${url}`;
  }

  try {
    const parsed = new URL(url);
    return `${parsed.protocol}//${parsed.host}`;
  } catch {
    throw new Error(
      `Invalid API URL "${value}". Use a full URL like http://192.168.100.21:8080`,
    );
  }
}

function readEnvApiBase(): string {
  const raw =
    import.meta.env.VITE_API_BASE_URL?.trim() ||
    import.meta.env.VITE_API_URL?.trim() ||
    '';
  if (!raw) {
    return '';
  }
  return normalizeBaseUrl(raw);
}

function inferLanApiBase(): string {
  if (typeof window === 'undefined') {
    return '';
  }

  const { protocol, hostname } = window.location;
  if (isLocalHost(hostname)) {
    return '';
  }

  const backendPort = import.meta.env.VITE_API_PORT?.trim() || '8080';
  return `${protocol}//${hostname}:${backendPort}`;
}

export function resolveApiBase(): string {
  const envBase = readEnvApiBase();
  if (envBase) {
    return envBase;
  }

  if (import.meta.env.DEV) {
    return inferLanApiBase();
  }

  return '';
}

export function buildApiUrl(path: string): string {
  const normalizedPath = path.startsWith('/') ? path : `/${path}`;
  const base = resolveApiBase();

  if (!base) {
    return normalizedPath;
  }

  return new URL(normalizedPath, `${base}/`).href;
}

export function resolveWebSocketBase(): string {
  const apiBase = resolveApiBase();
  if (apiBase) {
    return apiBase.replace(/^http/i, 'ws');
  }

  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
  return `${protocol}//${window.location.host}`;
}

export function getApiConnectionMode(): 'env' | 'lan-direct' | 'dev-proxy' | 'prod-proxy' {
  if (readEnvApiBase()) {
    return 'env';
  }
  if (import.meta.env.DEV) {
    return inferLanApiBase() ? 'lan-direct' : 'dev-proxy';
  }
  return 'prod-proxy';
}

export function getApiDebugInfo(): {
  mode: string;
  apiBase: string;
  sampleLoginUrl: string;
  webSocketBase: string;
  pageOrigin: string;
} {
  const apiBase = resolveApiBase();
  return {
    mode: getApiConnectionMode(),
    apiBase: apiBase || '(same-origin via proxy)',
    sampleLoginUrl: buildApiUrl('/api/auth/login'),
    webSocketBase: resolveWebSocketBase(),
    pageOrigin: typeof window !== 'undefined' ? window.location.origin : 'ssr',
  };
}
