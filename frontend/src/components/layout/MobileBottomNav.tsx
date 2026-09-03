import { NavLink } from 'react-router-dom';
import { BOTTOM_NAV_MORE_ICON, type NavItem } from '../../config/navItems';

interface MobileBottomNavProps {
  bottomItems: NavItem[];
  showMore: boolean;
  moreActive: boolean;
  unreadCount: number;
  onOpenMore: () => void;
}

export function MobileBottomNav({
  bottomItems,
  showMore,
  moreActive,
  unreadCount,
  onOpenMore,
}: MobileBottomNavProps) {
  return (
    <nav className="layout__bottom-nav" aria-label="Mobile navigation">
      {bottomItems.map((item) => (
        <NavLink
          key={item.to}
          to={item.to}
          className={({ isActive }) =>
            `layout__bottom-nav-link${isActive ? ' layout__bottom-nav-link--active' : ''}`
          }
        >
          <span className="layout__bottom-nav-icon" aria-hidden="true">
            {item.icon ?? '•'}
          </span>
          <span className="layout__bottom-nav-label">
            {item.shortLabel}
            {item.to === '/notifications' && unreadCount > 0 && (
              <span className="nav-badge nav-badge--bottom">{unreadCount > 99 ? '99+' : unreadCount}</span>
            )}
          </span>
        </NavLink>
      ))}
      {showMore && (
        <button
          type="button"
          className={`layout__bottom-nav-link layout__bottom-nav-link--button${
            moreActive ? ' layout__bottom-nav-link--active' : ''
          }`}
          onClick={onOpenMore}
          aria-expanded={moreActive}
          aria-haspopup="dialog"
        >
          <span className="layout__bottom-nav-icon" aria-hidden="true">
            {BOTTOM_NAV_MORE_ICON}
          </span>
          <span className="layout__bottom-nav-label">More</span>
        </button>
      )}
    </nav>
  );
}
