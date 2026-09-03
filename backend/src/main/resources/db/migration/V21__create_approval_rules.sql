-- =============================================================================
-- V21: Configurable approval rules + unified approval inbox support
-- Phase 20 — documents who may approve which workflow types per business.
-- =============================================================================

CREATE TABLE approval_rules (
    id                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    business_id         BIGINT UNSIGNED NOT NULL,
    code                VARCHAR(50)     NOT NULL,
    name                VARCHAR(100)    NOT NULL,
    description         VARCHAR(500)    NULL,
    entity_type         VARCHAR(32)     NOT NULL,
    required_permission VARCHAR(100)    NOT NULL,
    min_abs_quantity    DECIMAL(19, 4)  NULL,
    enabled             TINYINT(1)      NOT NULL DEFAULT 1,
    priority            INT             NOT NULL DEFAULT 100,
    created_at          TIMESTAMP(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at          TIMESTAMP(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),
    UNIQUE KEY uk_approval_rules_business_code (business_id, code),
    INDEX idx_approval_rules_business (business_id),
    INDEX idx_approval_rules_entity (business_id, entity_type, enabled),
    CONSTRAINT fk_approval_rules_business FOREIGN KEY (business_id) REFERENCES businesses (id),
    CONSTRAINT chk_approval_rules_entity_type CHECK (
        entity_type IN ('INVENTORY_ADJUSTMENT', 'STOCK_TRANSFER', 'IMPORT_ORDER', 'STOCKTAKE')
    ),
    CONSTRAINT chk_approval_rules_priority CHECK (priority >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO permissions (code, name, module) VALUES
    ('approval:view', 'View approval inbox and rules', 'approvals');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code IN ('approval:view', 'approval:manage')
WHERE r.code = 'GENERAL_MANAGER';

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code = 'approval:view'
WHERE r.code IN ('WAREHOUSE_MANAGER', 'SHOP_MANAGER', 'ACCOUNTANT', 'AUDITOR');

-- Default MDL approval rules (mirror current hard-coded permissions)
INSERT INTO approval_rules (business_id, code, name, description, entity_type, required_permission, priority)
SELECT b.id, 'ADJ-DEFAULT', 'Stock adjustment approval', 'Worker adjustment requests require manager sign-off',
       'INVENTORY_ADJUSTMENT', 'inventory:adjust', 100
FROM businesses b WHERE b.code = 'MDL';

INSERT INTO approval_rules (business_id, code, name, description, entity_type, required_permission, priority)
SELECT b.id, 'XFER-DEFAULT', 'Transfer approval', 'Stock transfer requests require approver sign-off',
       'STOCK_TRANSFER', 'transfer:approve', 100
FROM businesses b WHERE b.code = 'MDL';

INSERT INTO approval_rules (business_id, code, name, description, entity_type, required_permission, priority)
SELECT b.id, 'IMPORT-DEFAULT', 'Import approval', 'Submitted import orders require approval before receiving',
       'IMPORT_ORDER', 'import:approve', 100
FROM businesses b WHERE b.code = 'MDL';

INSERT INTO approval_rules (business_id, code, name, description, entity_type, required_permission, priority)
SELECT b.id, 'COUNT-DEFAULT', 'Stocktake approval', 'Submitted stock counts require manager approval before ledger update',
       'STOCKTAKE', 'inventory:adjust', 100
FROM businesses b WHERE b.code = 'MDL';
