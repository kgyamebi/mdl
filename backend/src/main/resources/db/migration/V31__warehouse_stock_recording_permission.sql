-- V31: Allow workers and managers to record warehouse stock movements at any business location.

INSERT INTO permissions (code, name, module)
SELECT 'inventory:record:warehouse', 'Record warehouse stock in/out', 'inventory'
WHERE NOT EXISTS (
    SELECT 1 FROM permissions WHERE code = 'inventory:record:warehouse'
);

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code = 'inventory:record:warehouse'
WHERE r.code IN ('OWNER', 'GENERAL_MANAGER', 'WAREHOUSE_MANAGER', 'SHOP_MANAGER', 'SHOP_WORKER')
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
