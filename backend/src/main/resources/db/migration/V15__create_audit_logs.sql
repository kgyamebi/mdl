-- =============================================================================
-- V15: Audit trail — append-only log of important business and security actions
-- =============================================================================

CREATE TABLE audit_logs (
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    business_id     BIGINT UNSIGNED NOT NULL,
    user_id         BIGINT UNSIGNED NULL,
    action          VARCHAR(64)     NOT NULL,
    module          VARCHAR(32)     NOT NULL,
    entity_type     VARCHAR(64)     NULL,
    entity_id       BIGINT UNSIGNED NULL,
    entity_ref      VARCHAR(100)    NULL,
    summary         VARCHAR(1000)   NOT NULL,
    details         JSON            NULL,
    ip_address      VARCHAR(45)     NULL,
    user_agent      VARCHAR(500)    NULL,
    created_at      TIMESTAMP(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),
    INDEX idx_audit_logs_business (business_id),
    INDEX idx_audit_logs_user (user_id),
    INDEX idx_audit_logs_module (module),
    INDEX idx_audit_logs_action (action),
    INDEX idx_audit_logs_entity (entity_type, entity_id),
    INDEX idx_audit_logs_created (created_at),
    CONSTRAINT fk_audit_logs_business FOREIGN KEY (business_id) REFERENCES businesses (id),
    CONSTRAINT fk_audit_logs_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Grant audit/report permissions to operational roles (OWNER already has all)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code IN ('audit:view', 'report:view', 'report:export')
WHERE r.code IN ('GENERAL_MANAGER', 'ACCOUNTANT', 'AUDITOR');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code IN ('audit:view', 'report:view')
WHERE r.code = 'SHOP_MANAGER';
