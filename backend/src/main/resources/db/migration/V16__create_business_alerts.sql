-- =============================================================================
-- V16: Business alerts — anomaly detection and owner attention center
-- =============================================================================

CREATE TABLE business_alerts (
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    business_id     BIGINT UNSIGNED NOT NULL,
    alert_type      VARCHAR(64)     NOT NULL,
    severity        VARCHAR(16)     NOT NULL,
    module          VARCHAR(32)     NOT NULL,
    title           VARCHAR(255)    NOT NULL,
    summary         VARCHAR(1000)   NOT NULL,
    entity_type     VARCHAR(64)     NULL,
    entity_id       BIGINT UNSIGNED NULL,
    entity_ref      VARCHAR(100)    NULL,
    details         JSON            NULL,
    dedupe_key      VARCHAR(255)    NULL,
    status          VARCHAR(16)     NOT NULL DEFAULT 'OPEN',
    acknowledged_by BIGINT UNSIGNED NULL,
    acknowledged_at TIMESTAMP(6)    NULL,
    resolved_at     TIMESTAMP(6)    NULL,
    created_at      TIMESTAMP(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at      TIMESTAMP(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),
    INDEX idx_business_alerts_business (business_id),
    INDEX idx_business_alerts_type (alert_type),
    INDEX idx_business_alerts_severity (severity),
    INDEX idx_business_alerts_status (status),
    INDEX idx_business_alerts_dedupe (business_id, dedupe_key),
    INDEX idx_business_alerts_created (created_at),
    CONSTRAINT fk_business_alerts_business FOREIGN KEY (business_id) REFERENCES businesses (id),
    CONSTRAINT fk_business_alerts_ack_by FOREIGN KEY (acknowledged_by) REFERENCES users (id),
    CONSTRAINT chk_business_alerts_severity CHECK (severity IN ('INFO', 'WARNING', 'CRITICAL')),
    CONSTRAINT chk_business_alerts_status CHECK (status IN ('OPEN', 'ACKNOWLEDGED', 'RESOLVED', 'DISMISSED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO permissions (code, name, module) VALUES
    ('alert:view',         'View business alerts', 'alerts'),
    ('alert:acknowledge',  'Acknowledge alerts',   'alerts');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code IN ('alert:view', 'alert:acknowledge')
WHERE r.code IN ('GENERAL_MANAGER', 'AUDITOR');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code = 'alert:view'
WHERE r.code = 'SHOP_MANAGER';
