-- Sync OWNER and SUPER_ADMIN with all permissions.
-- V3 granted every permission at seed time; later migrations added new permissions
-- without updating these roles, causing 403s on alerts, approvals, and similar endpoints.

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.code IN ('OWNER', 'SUPER_ADMIN')
  AND NOT EXISTS (
      SELECT 1
      FROM role_permissions rp
      WHERE rp.role_id = r.id
        AND rp.permission_id = p.id
  );
