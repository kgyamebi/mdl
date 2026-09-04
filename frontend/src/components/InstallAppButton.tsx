import { useState } from 'react';

import { usePwaInstall, type InstallPlatform } from '../hooks/usePwaInstall';



interface InstallAppButtonProps {

  variant?: 'sidebar' | 'mobile';

}



function InstallInstructions({ platform }: { platform: InstallPlatform }) {

  if (platform === 'ios') {

    return (

      <ol className="install-app-modal__steps">

        <li>Tap the <strong>Share</strong> button in Safari (square with arrow).</li>

        <li>Scroll down and tap <strong>Add to Home Screen</strong>.</li>

        <li>Tap <strong>Add</strong> — MDL opens like a native app.</li>

      </ol>

    );

  }



  if (platform === 'android') {

    return (

      <ol className="install-app-modal__steps">

        <li>Open the browser menu (three dots).</li>

        <li>Tap <strong>Install app</strong> or <strong>Add to Home screen</strong>.</li>

        <li>Confirm — MDL will appear on your home screen.</li>

      </ol>

    );

  }



  return (

    <ol className="install-app-modal__steps">

      <li>In Chrome or Edge, click the <strong>Install</strong> icon in the address bar, or open the browser menu.</li>

      <li>Choose <strong>Install Modern Dream Light</strong> (or <strong>Apps → Install this site as an app</strong>).</li>

      <li>Launch MDL from your desktop or taskbar for quick access.</li>

    </ol>

  );

}



function installTitle(platform: InstallPlatform): string {

  switch (platform) {

    case 'ios':

      return 'Install on iPhone or iPad';

    case 'android':

      return 'Install on Android';

    default:

      return 'Install on this device';

  }

}



export function InstallAppButton({ variant = 'sidebar' }: InstallAppButtonProps) {

  const { platform, canInstallNative, canShowInstall, install } = usePwaInstall();

  const [showHelp, setShowHelp] = useState(false);



  if (!canShowInstall) {

    return null;

  }



  async function handleInstallClick() {

    if (canInstallNative) {

      const accepted = await install();

      if (!accepted) {

        setShowHelp(true);

      }

      return;

    }

    setShowHelp(true);

  }



  return (

    <>

      <button

        type="button"

        className={`btn btn--ghost install-app-button install-app-button--${variant}`}

        onClick={handleInstallClick}

      >

        Install MDL App 📲

      </button>



      {showHelp && (

        <div className="install-app-modal" role="dialog" aria-modal="true" aria-labelledby="install-help-title">

          <button

            type="button"

            className="install-app-modal__backdrop"

            aria-label="Close install instructions"

            onClick={() => setShowHelp(false)}

          />

          <div className="install-app-modal__panel panel">

            <h2 id="install-help-title">{installTitle(platform)}</h2>

            <p className="muted">

              Install MDL on your device for one-tap access. Updates sync automatically when you are online.

            </p>

            <InstallInstructions platform={platform} />

            <div className="install-app-modal__actions">

              <button type="button" className="btn btn--primary" onClick={() => setShowHelp(false)}>

                Got it

              </button>

            </div>

          </div>

        </div>

      )}

    </>

  );

}

