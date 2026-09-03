import { useEffect, useState } from 'react';
import { NavLink, Outlet, useLocation } from 'react-router-dom';
import { useAuth } from '../../auth/AuthContext';
import {
  filterVisibleNavItems,
  groupNavItems,
  NAV_ITEMS,
  splitBottomNavItems,
} from '../../config/navItems';
import { useUnreadNotificationCount } from '../../hooks/useUnreadNotificationCount';
import { MobileBottomNav } from './MobileBottomNav';
import { MobileMoreMenu } from './MobileMoreMenu';
import { CopilotFloatingButton } from '../copilot/CopilotFloatingButton';
import { MdlLogo } from '../brand/MdlLogo';

export function AppLayout() {
  const { user, logout, hasAnyPermission } = useAuth();
  const location = useLocation();
  const unreadCount = useUnreadNotificationCount(location.pathname);
  const [moreOpen, setMoreOpen] = useState(false);

  const visibleNav = filterVisibleNavItems(NAV_ITEMS, hasAnyPermission);
  const groupedNav = groupNavItems(visibleNav);
  const { bottomItems, overflowItems } = splitBottomNavItems(visibleNav);
  const overflowPaths = new Set(overflowItems.map((item) => item.to));
  const moreActive = moreOpen || overflowPaths.has(location.pathname);

  useEffect(() => {
    setMoreOpen(false);
    window.scrollTo({ top: 0, left: 0, behavior: 'auto' });
  }, [location.pathname]);

  useEffect(() => {
    document.body.classList.toggle('mobile-more-open', moreOpen);
    return () => document.body.classList.remove('mobile-more-open');
  }, [moreOpen]);

  return (
    <div className="layout">
      <aside className="layout__sidebar">
        <div className="layout__brand">
          <MdlLogo variant="sidebar" />
          <strong className="layout__brand-business">{user?.businessName}</strong>
        </div>

        <nav className="layout__nav" aria-label="Main navigation">
          {groupedNav.map(({ group, items }) => (
            <div key={group.id} className="layout__nav-group">
              <p className="layout__nav-group-title">{group.label}</p>
              {items.map((item) => (
                <NavLink
                  key={item.to}
                  to={item.to}
                  className={({ isActive }) =>
                    `layout__nav-link${item.to === '/copilot' ? ' layout__nav-link--copilot' : ''}${
                      isActive ? ' layout__nav-link--active' : ''
                    }`
                  }
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
            </div>
          ))}
        </nav>

        <div className="layout__user">
          <div>
            <strong>{user?.fullName}</strong>
            <p className="muted">{user?.roles.join(', ')}</p>
          </div>
          <button type="button" className="btn btn--ghost" onClick={() => logout()}>
            Sign out
          </button>
        </div>
      </aside>

      <header className="layout__mobile-header">
        <div className="layout__mobile-brand">
          <MdlLogo variant="compact" />
          <strong className="layout__brand-business">{user?.businessName}</strong>
        </div>
        <p className="layout__mobile-user muted">{user?.fullName}</p>
      </header>

      <div className="layout__main">
        <main className="layout__content">
          <Outlet />
        </main>
      </div>

      <MobileBottomNav
        bottomItems={bottomItems}
        showMore
        moreActive={moreActive}
        moreOpen={moreOpen}
        unreadCount={unreadCount}
        onToggleMore={() => setMoreOpen((open) => !open)}
        onNavigate={() => setMoreOpen(false)}
      />

      <MobileMoreMenu
        open={moreOpen}
        overflowItems={overflowItems}
        user={user}
        unreadCount={unreadCount}
        onClose={() => setMoreOpen(false)}
        onSignOut={() => logout()}
      />

      <CopilotFloatingButton />
    </div>
  );
}
