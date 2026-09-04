import { useState } from 'react';
import { useInstallAppAction } from '../hooks/useInstallAppAction';
import { InstallHelpModal } from './InstallHelpModal';

const DISMISS_KEY = 'mdl_install_banner_dismissed';

function isBannerDismissed(): boolean {
  try {
    return localStorage.getItem(DISMISS_KEY) === '1';
  } catch {
    return false;
  }
}

function dismissBanner(): void {
  try {
    localStorage.setItem(DISMISS_KEY, '1');
  } catch {
    // ignore storage failures
  }
}

export function PostLoginInstallBanner() {
  const { platform, canShowInstall, showHelp, setShowHelp, triggerInstall } = useInstallAppAction();
  const [dismissed, setDismissed] = useState(() => isBannerDismissed());

  if (!canShowInstall || dismissed) {
    return showHelp ? (
      <InstallHelpModal platform={platform} open={showHelp} onClose={() => setShowHelp(false)} />
    ) : null;
  }

  return (
    <>
      <div className="pwa-banner pwa-banner--install post-login-install-banner" role="status">
        <div className="post-login-install-banner__copy">
          <strong>Install MDL on this device</strong>
          <span className="muted">
            Add the app to your home screen for one-tap access — no app store required.
          </span>
        </div>
        <div className="pwa-banner__actions">
          <button type="button" className="btn btn--primary" onClick={() => void triggerInstall()}>
            Install
          </button>
          <button
            type="button"
            className="btn btn--ghost"
            onClick={() => {
              dismissBanner();
              setDismissed(true);
            }}
          >
            Not now
          </button>
        </div>
      </div>
      <InstallHelpModal platform={platform} open={showHelp} onClose={() => setShowHelp(false)} />
    </>
  );
}
