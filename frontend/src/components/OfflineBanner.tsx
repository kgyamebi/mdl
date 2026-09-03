interface OfflineBannerProps {
  online: boolean;
}

export function OfflineBanner({ online }: OfflineBannerProps) {
  if (online) {
    return null;
  }

  return (
    <div className="pwa-banner pwa-banner--offline" role="alert">
      You are offline. Cached screens remain available; live data will refresh when reconnected.
    </div>
  );
}
