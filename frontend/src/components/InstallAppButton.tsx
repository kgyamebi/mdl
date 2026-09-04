import { useInstallAppAction } from '../hooks/useInstallAppAction';
import { InstallHelpModal } from './InstallHelpModal';

interface InstallAppButtonProps {
  variant?: 'sidebar' | 'mobile' | 'login';
}

export function InstallAppButton({ variant = 'sidebar' }: InstallAppButtonProps) {
  const { platform, canShowInstall, showHelp, setShowHelp, triggerInstall } = useInstallAppAction();

  if (!canShowInstall) {
    return null;
  }

  return (
    <>
      <button
        type="button"
        className={`btn btn--ghost install-app-button install-app-button--${variant}`}
        onClick={() => void triggerInstall()}
      >
        Install MDL App 📲
      </button>
      <InstallHelpModal platform={platform} open={showHelp} onClose={() => setShowHelp(false)} />
    </>
  );
}
