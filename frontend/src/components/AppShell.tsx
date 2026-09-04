import type { ReactNode } from 'react';
import { OfflineBanner } from './OfflineBanner';
import { PwaUpdatePrompt } from './PwaUpdatePrompt';
import { useOnlineStatus } from '../hooks/useOnlineStatus';

interface AppShellProps {
  children: ReactNode;
}

export function AppShell({ children }: AppShellProps) {
  const online = useOnlineStatus();

  return (
    <div className="app-shell">
      <OfflineBanner online={online} />
      <PwaUpdatePrompt />
      {children}
    </div>
  );
}
