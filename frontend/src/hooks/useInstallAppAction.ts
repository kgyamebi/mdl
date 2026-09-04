import { useCallback, useState } from 'react';
import { usePwaInstall } from './usePwaInstall';

export function useInstallAppAction() {
  const { platform, canInstallNative, canShowInstall, install } = usePwaInstall();
  const [showHelp, setShowHelp] = useState(false);

  const triggerInstall = useCallback(async () => {
    if (canInstallNative) {
      const accepted = await install();
      if (!accepted) {
        setShowHelp(true);
      }
      return accepted;
    }

    setShowHelp(true);
    return false;
  }, [canInstallNative, install]);

  return {
    platform,
    canShowInstall,
    showHelp,
    setShowHelp,
    triggerInstall,
  };
}
