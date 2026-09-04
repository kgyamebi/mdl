import type { InstallPlatform } from '../hooks/usePwaInstall';

function InstallInstructions({ platform }: { platform: InstallPlatform }) {
  if (platform === 'ios') {
    return (
      <ol className="install-app-modal__steps">
        <li>
          Tap the <strong>Share</strong> button in Safari (square with arrow).
        </li>
        <li>
          Scroll down and tap <strong>Add to Home Screen</strong>.
        </li>
        <li>
          Tap <strong>Add</strong> — MDL opens like a native app.
        </li>
      </ol>
    );
  }

  if (platform === 'android') {
    return (
      <ol className="install-app-modal__steps">
        <li>Open the browser menu (three dots).</li>
        <li>
          Tap <strong>Install app</strong> or <strong>Add to Home screen</strong>.
        </li>
        <li>Confirm — MDL will appear on your home screen.</li>
      </ol>
    );
  }

  return (
    <ol className="install-app-modal__steps">
      <li>
        In Chrome or Edge, click the <strong>Install</strong> icon in the address bar, or open the
        browser menu.
      </li>
      <li>
        Choose <strong>Install Modern Dream Light</strong> (or{' '}
        <strong>Apps → Install this site as an app</strong>).
      </li>
      <li>Launch MDL from your desktop or taskbar for quick access.</li>
    </ol>
  );
}

export function installTitle(platform: InstallPlatform): string {
  switch (platform) {
    case 'ios':
      return 'Install on iPhone or iPad';
    case 'android':
      return 'Install on Android';
    default:
      return 'Install on this device';
  }
}

interface InstallHelpModalProps {
  platform: InstallPlatform;
  open: boolean;
  onClose: () => void;
}

export function InstallHelpModal({ platform, open, onClose }: InstallHelpModalProps) {
  if (!open) {
    return null;
  }

  return (
    <div className="install-app-modal" role="dialog" aria-modal="true" aria-labelledby="install-help-title">
      <button
        type="button"
        className="install-app-modal__backdrop"
        aria-label="Close install instructions"
        onClick={onClose}
      />
      <div className="install-app-modal__panel panel">
        <h2 id="install-help-title">{installTitle(platform)}</h2>
        <p className="muted">
          Install MDL on your device for one-tap access. Updates sync automatically when you are
          online.
        </p>
        <InstallInstructions platform={platform} />
        <div className="install-app-modal__actions">
          <button type="button" className="btn btn--primary" onClick={onClose}>
            Got it
          </button>
        </div>
      </div>
    </div>
  );
}
