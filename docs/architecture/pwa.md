# PWA — Phase 25

## Purpose

Make MDL Platform **installable** on phones and tablets as a Progressive Web App — offline app shell, home-screen icon, and update prompts — without a separate native app store build.

## Features

| Feature | Implementation |
|---------|----------------|
| Web app manifest | `vite-plugin-pwa` — name, theme, icons, standalone display |
| Service worker | Precaches JS/CSS/HTML; network-first cache for `/api/health` only |
| Install prompt | Captures `beforeinstallprompt` for Add to Home Screen |
| Update prompt | Prompts when a new service worker is ready |
| Offline banner | Shows when `navigator.onLine` is false |

Authenticated API calls are **not** cached — only static assets and the public health check.

## Development

```powershell
cd frontend
npm install
npm run dev
```

PWA dev mode is enabled (`devOptions.enabled: true`) so the service worker registers during local development.

## Production build

```powershell
npm run build
npm run preview
```

Serve the `dist/` folder over HTTPS in production (required for install prompts on most browsers).

## Icons

- `frontend/public/icon.svg` — stylized **M** mark on dark gradient (favicon / PWA)
- In-app wordmark is rendered in React (`MdlLogo`) — **M** + **modern** + boxed **DL**, transparent, adapts to UI theme

## Related

- [Production readiness](./production-readiness.md) — backend deployment
- Root `README.md` — frontend quick start
