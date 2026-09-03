import { useEffect, useState } from 'react';

interface BeforeInstallPromptEvent extends Event {
  prompt: () => Promise<void>;
  userChoice: Promise<{ outcome: 'accepted' | 'dismissed' }>;
}

export function PwaInstallPrompt() {
  const [deferredPrompt, setDeferredPrompt] = useState<BeforeInstallPromptEvent | null>(null);
  const [dismissed, setDismissed] = useState(false);

  useEffect(() => {
    const handler = (event: Event) => {
      event.preventDefault();
      setDeferredPrompt(event as BeforeInstallPromptEvent);
    };

    window.addEventListener('beforeinstallprompt', handler);
    return () => window.removeEventListener('beforeinstallprompt', handler);
  }, []);

  if (!deferredPrompt || dismissed) {
    return null;
  }

  const install = async () => {
    await deferredPrompt.prompt();
    await deferredPrompt.userChoice;
    setDeferredPrompt(null);
    setDismissed(true);
  };

  return (
    <div className="pwa-banner pwa-banner--install" role="status">
      <span>Install MDL Platform on this device for quick access.</span>
      <div className="pwa-banner__actions">
        <button type="button" className="btn btn--primary" onClick={install}>
          Install
        </button>
        <button type="button" className="btn btn--ghost" onClick={() => setDismissed(true)}>
          Not now
        </button>
      </div>
    </div>
  );
}
