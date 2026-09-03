-- =============================================================================
-- V12: Temporary permissions — task-based access to restricted main warehouses
-- Main warehouse access is granted per task, not via a permanent broad flag.
-- =============================================================================

CREATE TABLE temporary_permissions (
    id                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    business_id         BIGINT UNSIGNED NOT NULL,
    user_id             BIGINT UNSIGNED NOT NULL,
    permission_code     VARCHAR(100)    NOT NULL,
    location_id         BIGINT UNSIGNED NOT NULL,
    reference_type      VARCHAR(32)     NULL,
    reference_id        BIGINT UNSIGNED NULL,
    reason              VARCHAR(500)    NULL,
    granted_by          BIGINT UNSIGNED NOT NULL,
    expires_at          TIMESTAMP(6)    NOT NULL,
    revoked_at          TIMESTAMP(6)    NULL,
    revoked_by          BIGINT UNSIGNED NULL,
    revoke_reason       VARCHAR(500)    NULL,
    status              VARCHAR(32)     NOT NULL DEFAULT 'ACTIVE',
    created_at          TIMESTAMP(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at          TIMESTAMP(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),
    INDEX idx_temp_permissions_business (business_id),
    INDEX idx_temp_permissions_user (user_id),
    INDEX idx_temp_permissions_location (location_id),
    INDEX idx_temp_permissions_status (status),
    INDEX idx_temp_permissions_expires (expires_at),
    INDEX idx_temp_permissions_reference (reference_type, reference_id),
    CONSTRAINT fk_temp_permissions_business FOREIGN KEY (business_id) REFERENCES businesses (id),
    CONSTRAINT fk_temp_permissions_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_temp_permissions_location FOREIGN KEY (location_id) REFERENCES locations (id),
    CONSTRAINT fk_temp_permissions_granted_by FOREIGN KEY (granted_by) REFERENCES users (id),
    CONSTRAINT fk_temp_permissions_revoked_by FOREIGN KEY (revoked_by) REFERENCES users (id),
    CONSTRAINT chk_temp_permissions_status CHECK (status IN ('ACTIVE', 'EXPIRED', 'REVOKED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
