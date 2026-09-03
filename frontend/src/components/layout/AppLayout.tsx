import { NavLink, Outlet } from 'react-router-dom';
import { useAuth } from '../../auth/AuthContext';

const NAV_ITEMS = [
  { to: '/dashboard', label: 'Dashboard', permissions: ['alert:view', 'inventory:view'] },
  { to: '/inventory', label: 'Inventory', permissions: ['inventory:view'] },
  { to: '/products', label: 'Products', permissions: ['product:view'] },
  { to: '/transfers', label: 'Transfers', permissions: ['transfer:view'] },
  { to: '/imports', label: 'Imports', permissions: ['import:view'] },
  { to: '/sales', label: 'Sales', permissions: ['sale:view'] },
  { to: '/approvals', label: 'Approvals', permissions: ['approval:view'] },
] as const;

export function AppLayout() {
  const { user, logout, hasAnyPermission } = useAuth();

  const visibleNav = NAV_ITEMS.filter((item) => hasAnyPermission(...item.permissions));

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
              {item.label}
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
