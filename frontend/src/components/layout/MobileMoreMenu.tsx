import { useEffect } from 'react';
import { InstallAppButton } from '../InstallAppButton';
import { NavLink } from 'react-router-dom';
import type { NavItem } from '../../config/navItems';
import type { AuthUser } from '../../types/api';

interface MobileMoreMenuProps {
  open: boolean;
  overflowItems: NavItem[];
  user: AuthUser | null;
  unreadCount: number;
  onClose: () => void;
  onSignOut: () => void;
}

export function MobileMoreMenu({
  open,
  overflowItems,
  user,
  unreadCount,
  onClose,
  onSignOut,
}: MobileMoreMenuProps) {
  useEffect(() => {
    if (!open) {
      return;
    }

    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === 'Escape') {
        onClose();
      }
    }

    document.addEventListener('keydown', handleKeyDown);
    return () => document.removeEventListener('keydown', handleKeyDown);
  }, [open, onClose]);

  if (!open) {
    return null;
  }

  return (
    <div className="mobile-more" role="presentation">
      <button type="button" className="mobile-more__backdrop" aria-label="Close menu" onClick={onClose} />
      <div className="mobile-more__panel" role="dialog" aria-modal="true" aria-label="More navigation">
        <header className="mobile-more__header">
          <div>
            <strong>{user?.fullName}</strong>
            <p className="muted">{user?.roles.join(', ')}</p>
          </div>
          <button type="button" className="btn btn--ghost" onClick={onClose}>
            Close
          </button>
        </header>

        {overflowItems.length > 0 && (
          <nav className="mobile-more__nav" aria-label="Additional modules">
            {overflowItems.map((item) => (
              <NavLink
                key={item.to}
                to={item.to}
                className={({ isActive }) =>
                  `mobile-more__link${item.to === '/copilot' ? ' mobile-more__link--copilot' : ''}${
                    isActive ? ' mobile-more__link--active' : ''
                  }`
                }
                onClick={onClose}
              >
                <span className="layout__nav-label">
                  {item.icon && (
                    <span className="layout__nav-icon" aria-hidden="true">
                      {item.icon}
                    </span>
                  )}
                  {item.label}
                  {item.to === '/notifications' && unreadCount > 0 && (
                    <span className="nav-badge">{unreadCount > 99 ? '99+' : unreadCount}</span>
                  )}
                </span>
              </NavLink>
            ))}
          </nav>
        )}

        <div className="mobile-more__actions">
          <InstallAppButton variant="mobile" />
          <button
            type="button"
            className="btn btn--ghost"
            onClick={() => {
              onClose();
              onSignOut();
            }}
          >
            Sign out
          </button>
        </div>
      </div>
    </div>
  );
}
