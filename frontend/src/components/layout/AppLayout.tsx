import { NavLink, Outlet, useLocation } from 'react-router-dom';
import { useAuth } from '../../auth/AuthContext';
import { useUnreadNotificationCount } from '../../hooks/useUnreadNotificationCount';

const NAV_ITEMS: Array<{
  to: string;
  label: string;
  permissions: readonly string[];
  showAlways?: boolean;
}> = [
  { to: '/dashboard', label: 'Dashboard', permissions: ['alert:view', 'inventory:view'] },
  { to: '/inventory', label: 'Inventory', permissions: ['inventory:view'] },
  { to: '/products', label: 'Products', permissions: ['product:view'] },
  { to: '/transfers', label: 'Transfers', permissions: ['transfer:view'] },
  { to: '/imports', label: 'Imports', permissions: ['import:view'] },
  { to: '/sales', label: 'Sales', permissions: ['sale:view'] },
  { to: '/reports', label: 'Reports', permissions: ['report:export'] },
  { to: '/approvals', label: 'Approvals', permissions: ['approval:view'] },
  { to: '/notifications', label: 'Notifications', permissions: [], showAlways: true },
];

export function AppLayout() {
  const { user, logout, hasAnyPermission } = useAuth();
  const location = useLocation();
  const unreadCount = useUnreadNotificationCount(location.pathname);

  const visibleNav = NAV_ITEMS.filter(
    (item) => item.showAlways || hasAnyPermission(...item.permissions),
  );

  return (
    <div className="layout">
      <aside className="layout__sidebar">
        <div className="layout__brand">
          <p className="eyebrow">MDL Platform</p>
          <strong>{user?.businessName}</strong>
        </div>

        <nav className="layout__nav" aria-label="Main navigation">
          {visibleNav.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              className={({ isActive }) =>
                `layout__nav-link${isActive ? ' layout__nav-link--active' : ''}`
              }
            >
              <span className="layout__nav-label">
                {item.label}
                {item.to === '/notifications' && unreadCount > 0 && (
                  <span className="nav-badge">{unreadCount > 99 ? '99+' : unreadCount}</span>
                )}
              </span>
            </NavLink>
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

      <div className="layout__main">
        <main className="layout__content">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
