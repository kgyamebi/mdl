const ROLE_LABELS: Record<string, string> = {
  OWNER: 'Owner',
  SUPER_ADMIN: 'Super Admin',
  GENERAL_MANAGER: 'General Manager',
  WAREHOUSE_MANAGER: 'Warehouse Manager',
  SHOP_MANAGER: 'Shop Manager',
  SHOP_WORKER: 'Shop Worker',
  SALES_STAFF: 'Sales Staff',
  IMPORT_RECEIVING_STAFF: 'Import Receiving Staff',
  ACCOUNTANT: 'Accountant',
  AUDITOR: 'Auditor',
  VIEWER: 'Viewer',
};

export function formatRoleName(code: string): string {
  if (ROLE_LABELS[code]) {
    return ROLE_LABELS[code];
  }
  return code
    .toLowerCase()
    .split('_')
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join(' ');
}

export function formatRoleList(codes: string[]): string {
  return codes.map(formatRoleName).join(', ');
}
