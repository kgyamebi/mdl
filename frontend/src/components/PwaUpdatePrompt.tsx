import { useRegisterSW } from 'virtual:pwa-register/react';

export function PwaUpdatePrompt() {
  const {
    needRefresh: [needRefresh, setNeedRefresh],
    updateServiceWorker,
  } = useRegisterSW({
    onRegistered(registration) {
      if (registration) {
        window.setInterval(() => registration.update(), 60 * 60 * 1000);
      }
    },
  });

  if (!needRefresh) {
    return null;
  }

  return (
    <div className="pwa-banner pwa-banner--update" role="status">
      <span>A new version of MDL Platform is available.</span>
      <div className="pwa-banner__actions">
        <button type="button" className="btn btn--primary" onClick={() => updateServiceWorker(true)}>
          Update
        </button>
        <button type="button" className="btn btn--ghost" onClick={() => setNeedRefresh(false)}>
          Later
        </button>
      </div>
    </div>
  );
}
