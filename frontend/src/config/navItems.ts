export type NavGroupId = 'overview' | 'operations' | 'logistics' | 'management';

export interface NavGroup {
  id: NavGroupId;
  label: string;
}

export interface NavItem {
  to: string;
  label: string;
  shortLabel: string;
  icon?: string;
  group: NavGroupId;
  permissions: readonly string[];
  showAlways?: boolean;
}

export const NAV_GROUPS: NavGroup[] = [
  { id: 'overview', label: 'Overview' },
  { id: 'operations', label: 'Operations' },
  { id: 'logistics', label: 'Logistics' },
  { id: 'management', label: 'Management' },
];

export const NAV_ITEMS: NavItem[] = [
  {
    to: '/dashboard',
    label: 'Dashboard',
    shortLabel: 'Home',
    icon: '🏠',
    group: 'overview',
    permissions: ['alert:view', 'inventory:view'],
  },
  {
    to: '/inventory',
    label: 'Inventory',
    shortLabel: 'Stock',
    icon: '📦',
    group: 'operations',
    permissions: ['inventory:view'],
  },
  {
    to: '/products',
    label: 'Products',
    shortLabel: 'Products',
    icon: '🏷️',
    group: 'operations',
    permissions: ['product:view'],
  },
  {
    to: '/sales',
    label: 'Sales',
    shortLabel: 'Sales',
    icon: '💰',
    group: 'operations',
    permissions: ['sale:view'],
  },
  {
    to: '/transfers',
    label: 'Transfers',
    shortLabel: 'Transfers',
    icon: '🚚',
    group: 'logistics',
    permissions: ['transfer:view'],
  },
  {
    to: '/imports',
    label: 'Imports',
    shortLabel: 'Imports',
    icon: '📥',
    group: 'logistics',
    permissions: ['import:view'],
  },
  {
    to: '/stocktakes',
    label: 'Stocktakes',
    shortLabel: 'Counts',
    icon: '📋',
    group: 'operations',
    permissions: ['stock:count'],
  },
  {
    to: '/reports',
    label: 'Reports',
    shortLabel: 'Reports',
    icon: '📊',
    group: 'management',
    permissions: ['report:export'],
  },
  {
    to: '/approvals',
    label: 'Approvals',
    shortLabel: 'Approvals',
    icon: '✅',
    group: 'management',
    permissions: ['approval:view'],
  },
  {
    to: '/notifications',
    label: 'Notifications',
    shortLabel: 'Inbox',
    icon: '🔔',
    group: 'management',
    permissions: [],
    showAlways: true,
  },
  {
    to: '/copilot',
    label: 'Copilot',
    shortLabel: 'Copilot',
    icon: '💬',
    group: 'management',
    permissions: ['copilot:use'],
  },
  {
    to: '/admin/users',
    label: 'Users',
    shortLabel: 'Users',
    icon: '👥',
    group: 'management',
    permissions: ['user:view'],
  },
  {
    to: '/admin/settings',
    label: 'Settings',
    shortLabel: 'Settings',
    icon: '⚙️',
    group: 'management',
    permissions: ['business:view'],
  },
  {
    to: '/admin/locations',
    label: 'Locations',
    shortLabel: 'Sites',
    icon: '🏢',
    group: 'management',
    permissions: ['business:view'],
  },
  {
    to: '/admin/approval-rules',
    label: 'Approval rules',
    shortLabel: 'Rules',
    icon: '📜',
    group: 'management',
    permissions: ['approval:view'],
  },
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

export function groupNavItems(items: NavItem[]): { group: NavGroup; items: NavItem[] }[] {
  return NAV_GROUPS.map((group) => ({
    group,
    items: items.filter((item) => item.group === group.id),
  })).filter((entry) => entry.items.length > 0);
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
