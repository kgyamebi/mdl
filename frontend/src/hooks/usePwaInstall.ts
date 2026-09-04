import { useCallback, useEffect, useState } from 'react';



interface BeforeInstallPromptEvent extends Event {

  prompt: () => Promise<void>;

  userChoice: Promise<{ outcome: 'accepted' | 'dismissed' }>;

}



export type InstallPlatform = 'ios' | 'android' | 'desktop';



function isStandaloneMode(): boolean {

  if (typeof window === 'undefined') {

    return false;

  }

  return (

    window.matchMedia('(display-mode: standalone)').matches

    || (window.navigator as Navigator & { standalone?: boolean }).standalone === true

  );

}



function isIosDevice(): boolean {

  if (typeof window === 'undefined') {

    return false;

  }

  const ua = window.navigator.userAgent;

  return /iPad|iPhone|iPod/i.test(ua)

    || (window.navigator.platform === 'MacIntel' && window.navigator.maxTouchPoints > 1);

}



function detectInstallPlatform(): InstallPlatform {

  if (isIosDevice()) {

    return 'ios';

  }

  if (/Android/i.test(window.navigator.userAgent)) {

    return 'android';

  }

  return 'desktop';

}



export function usePwaInstall() {

  const [deferredPrompt, setDeferredPrompt] = useState<BeforeInstallPromptEvent | null>(null);

  const [installed, setInstalled] = useState(isStandaloneMode);

  const platform = detectInstallPlatform();



  const canInstallNative = deferredPrompt != null;

  const canShowInstall = !installed;



  useEffect(() => {

    function handleBeforeInstall(event: Event) {

      event.preventDefault();

      setDeferredPrompt(event as BeforeInstallPromptEvent);

    }



    function handleAppInstalled() {

      setInstalled(true);

      setDeferredPrompt(null);

    }



    window.addEventListener('beforeinstallprompt', handleBeforeInstall);

    window.addEventListener('appinstalled', handleAppInstalled);



    return () => {

      window.removeEventListener('beforeinstallprompt', handleBeforeInstall);

      window.removeEventListener('appinstalled', handleAppInstalled);

    };

  }, []);



  const install = useCallback(async () => {

    if (!deferredPrompt) {

      return false;

    }

    await deferredPrompt.prompt();

    const choice = await deferredPrompt.userChoice;

    setDeferredPrompt(null);

    if (choice.outcome === 'accepted') {

      setInstalled(true);

      return true;

    }

    return false;

  }, [deferredPrompt]);



  return {

    installed,

    platform,

    isIos: platform === 'ios',

    canInstallNative,

    canShowInstall,

    install,

  };

}

