-- Shop and warehouse managers need a dedicated right to approve physical counts
-- without also granting unrestricted inventory:adjust.

INSERT INTO permissions (code, name, module) VALUES
    ('stocktake:approve', 'Approve stocktakes and post count variances', 'inventory');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code = 'stocktake:approve'
WHERE r.code IN ('OWNER', 'GENERAL_MANAGER', 'WAREHOUSE_MANAGER', 'SHOP_MANAGER');

UPDATE approval_rules
SET required_permission = 'stocktake:approve'
WHERE entity_type = 'STOCKTAKE';
