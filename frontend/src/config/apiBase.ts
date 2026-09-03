/**
 * Resolves the API base URL for the current runtime.
 *
 * - Vite dev (port 5173): empty string → same-origin via Vite proxy (works on phone + PC)
 * - Production / docker nginx: empty string → nginx proxies /api
 * - Override anytime with VITE_API_BASE_URL
 */
export function resolveApiBase(): string {
  const envBase = import.meta.env.VITE_API_BASE_URL?.trim();
  if (envBase) {
    return envBase.replace(/\/$/, '');
  }

  return '';
}

export function resolveWebSocketBase(): string {
  const envBase = import.meta.env.VITE_API_BASE_URL?.trim();
  if (envBase) {
    return envBase.replace(/^http/i, 'ws').replace(/\/$/, '');
  }

  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
  return `${protocol}//${window.location.host}`;
}
