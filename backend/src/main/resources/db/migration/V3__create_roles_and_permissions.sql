-- =============================================================================
-- V3: Roles and permissions (RBAC foundation)
-- System roles are shared; business-specific custom roles can be added later.
-- =============================================================================

CREATE TABLE permissions (
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    code            VARCHAR(100)    NOT NULL,
    name            VARCHAR(255)    NOT NULL,
    description     VARCHAR(500)    NULL,
    module          VARCHAR(50)     NOT NULL,

    PRIMARY KEY (id),
    UNIQUE KEY uk_permissions_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE roles (
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    business_id     BIGINT UNSIGNED NULL,
    code            VARCHAR(50)     NOT NULL,
    name            VARCHAR(255)    NOT NULL,
    description     VARCHAR(500)    NULL,
    is_system       BOOLEAN         NOT NULL DEFAULT FALSE,
    status          VARCHAR(32)     NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMP(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at      TIMESTAMP(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),
    UNIQUE KEY uk_roles_business_code (business_id, code),
    CONSTRAINT fk_roles_business FOREIGN KEY (business_id) REFERENCES businesses (id),
    CONSTRAINT chk_roles_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE role_permissions (
    role_id         BIGINT UNSIGNED NOT NULL,
    permission_id   BIGINT UNSIGNED NOT NULL,

    PRIMARY KEY (role_id, permission_id),
    CONSTRAINT fk_rp_role FOREIGN KEY (role_id) REFERENCES roles (id),
    CONSTRAINT fk_rp_permission FOREIGN KEY (permission_id) REFERENCES permissions (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE user_roles (
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id         BIGINT UNSIGNED NOT NULL,
    role_id         BIGINT UNSIGNED NOT NULL,
    business_id     BIGINT UNSIGNED NOT NULL,
    assigned_at     TIMESTAMP(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    assigned_by     BIGINT UNSIGNED NULL,

    PRIMARY KEY (id),
    UNIQUE KEY uk_user_role_business (user_id, role_id, business_id),
    INDEX idx_user_roles_business (business_id),
    CONSTRAINT fk_ur_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_ur_role FOREIGN KEY (role_id) REFERENCES roles (id),
    CONSTRAINT fk_ur_business FOREIGN KEY (business_id) REFERENCES businesses (id),
    CONSTRAINT fk_ur_assigned_by FOREIGN KEY (assigned_by) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------------
-- Seed permissions (fine-grained — used with roles, locations, and tasks)
-- ---------------------------------------------------------------------------

INSERT INTO permissions (code, name, module) VALUES
    ('business:view',           'View business settings',           'business'),
    ('business:manage',         'Manage business settings',         'business'),
    ('user:view',               'View users',                       'users'),
    ('user:manage',             'Manage users',                     'users'),
    ('permission:grant',        'Grant permissions',                'security'),
    ('inventory:view',          'View inventory at assigned locations', 'inventory'),
    ('inventory:view:all',      'View inventory across all locations',  'inventory'),
    ('inventory:adjust:request','Request stock adjustments',        'inventory'),
    ('import:create',           'Create imports',                   'imports'),
    ('import:view',             'View imports',                     'imports'),
    ('import:receive',          'Receive imports (general)',        'imports'),
    ('import:receive:task',     'Receive imports (task-scoped)',    'imports'),
    ('import:verify',           'Verify import receiving',          'imports'),
    ('import:approve',          'Approve imports',                  'imports'),
    ('transfer:create',         'Create transfers',                 'transfers'),
    ('transfer:view',           'View transfers',                   'transfers'),
    ('transfer:approve',        'Approve transfers',                'transfers'),
    ('transfer:dispatch',       'Dispatch transfers',               'transfers'),
    ('transfer:receive',        'Receive transfers',                'transfers'),
    ('sale:create',             'Create sales',                     'sales'),
    ('sale:view',               'View sales',                       'sales'),
    ('sale:cancel',             'Cancel sales',                     'sales'),
    ('sale:refund',             'Process refunds',                  'sales'),
    ('stock:request',           'Request stock',                    'stock'),
    ('stock:count',             'Perform stock counts',             'inventory'),
    ('damage:report',           'Report damaged stock',             'inventory'),
    ('report:view',             'View reports',                     'reports'),
    ('report:export',           'Export reports',                   'reports'),
    ('audit:view',              'View audit logs',                  'audit'),
    ('security:view',           'View security alerts',             'security'),
    ('approval:manage',         'Manage approval rules',            'approvals');

-- ---------------------------------------------------------------------------
-- Seed system roles (business_id NULL = applies to all businesses)
-- ---------------------------------------------------------------------------

INSERT INTO roles (business_id, code, name, description, is_system) VALUES
    (NULL, 'OWNER',                  'Owner',                  'Full business visibility and control', TRUE),
    (NULL, 'SUPER_ADMIN',            'Super Admin',            'Technical administration', TRUE),
    (NULL, 'GENERAL_MANAGER',        'General Manager',        'Cross-location management', TRUE),
    (NULL, 'WAREHOUSE_MANAGER',      'Warehouse Manager',      'Manages assigned warehouses', TRUE),
    (NULL, 'IMPORT_RECEIVING_STAFF', 'Import Receiving Staff', 'Import receiving support', TRUE),
    (NULL, 'SHOP_MANAGER',           'Shop Manager',           'Manages assigned shop', TRUE),
    (NULL, 'SHOP_WORKER',            'Shop Worker',            'Shop floor operations', TRUE),
    (NULL, 'SALES_STAFF',            'Sales Staff',            'Point of sale operations', TRUE),
    (NULL, 'ACCOUNTANT',             'Accountant',             'Financial reporting access', TRUE),
    (NULL, 'AUDITOR',                'Auditor',                'Read-only audit access', TRUE),
    (NULL, 'VIEWER',                 'Viewer',                 'Read-only general access', TRUE);

-- Owner gets all permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.code = 'OWNER';

-- Viewer gets read-only permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code IN (
    'business:view', 'inventory:view', 'import:view', 'transfer:view',
    'sale:view', 'report:view'
)
WHERE r.code = 'VIEWER';

-- Shop worker permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code IN (
    'inventory:view', 'sale:create', 'sale:view', 'stock:request',
    'damage:report', 'transfer:receive'
)
WHERE r.code = 'SHOP_WORKER';

-- Import receiving staff — task-scoped receive only (enforced in service layer)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code IN ('import:view', 'import:receive:task')
WHERE r.code = 'IMPORT_RECEIVING_STAFF';
