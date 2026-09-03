export interface NavItem {
  to: string;
  label: string;
  shortLabel: string;
  icon?: string;
  permissions: readonly string[];
  showAlways?: boolean;
}

export const NAV_ITEMS: NavItem[] = [
  { to: '/dashboard', label: 'Dashboard', shortLabel: 'Home', icon: '🏠', permissions: ['alert:view', 'inventory:view'] },
  { to: '/inventory', label: 'Inventory', shortLabel: 'Stock', icon: '📦', permissions: ['inventory:view'] },
  { to: '/products', label: 'Products', shortLabel: 'Products', icon: '🏷️', permissions: ['product:view'] },
  { to: '/transfers', label: 'Transfers', shortLabel: 'Transfers', icon: '🚚', permissions: ['transfer:view'] },
  { to: '/imports', label: 'Imports', shortLabel: 'Imports', icon: '📥', permissions: ['import:view'] },
  { to: '/sales', label: 'Sales', shortLabel: 'Sales', icon: '💰', permissions: ['sale:view'] },
  { to: '/reports', label: 'Reports', shortLabel: 'Reports', icon: '📊', permissions: ['report:export'] },
  { to: '/approvals', label: 'Approvals', shortLabel: 'Approvals', icon: '✅', permissions: ['approval:view'] },
  {
    to: '/notifications',
    label: 'Notifications',
    shortLabel: 'Inbox',
    icon: '🔔',
    permissions: [],
    showAlways: true,
  },
  { to: '/copilot', label: 'Copilot', shortLabel: 'Copilot', icon: '💬', permissions: ['copilot:use'] },
];

/** Primary bottom-bar slots (permission-filtered at runtime). */
export const BOTTOM_NAV_PRIORITY = [
  '/dashboard',
  '/inventory',
  '/sales',
  '/notifications',
] as const;

export const BOTTOM_NAV_MORE_ICON = '☰';

export function filterVisibleNavItems(
  items: NavItem[],
  hasAnyPermission: (...permissions: string[]) => boolean,
): NavItem[] {
  return items.filter((item) => item.showAlways || hasAnyPermission(...item.permissions));
}

export function splitBottomNavItems(visibleItems: NavItem[]): {
  bottomItems: NavItem[];
  overflowItems: NavItem[];
} {
  const bottomItems = BOTTOM_NAV_PRIORITY.map((path) =>
    visibleItems.find((item) => item.to === path),
  ).filter((item): item is NavItem => item !== undefined);

  const bottomPaths = new Set(bottomItems.map((item) => item.to));
  const overflowItems = visibleItems.filter((item) => !bottomPaths.has(item.to));

  return { bottomItems, overflowItems };
}
