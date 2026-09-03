-- =============================================================================
-- V11: Import orders — supplier shipments into main warehouses
-- Receiving posts IMPORT_RECEIVE ledger rows (Phase 7 ledger).
-- =============================================================================

CREATE TABLE imports (
    id                      BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    business_id             BIGINT UNSIGNED NOT NULL,
    import_number           VARCHAR(50)     NOT NULL,
    supplier_name           VARCHAR(255)    NOT NULL,
    supplier_reference      VARCHAR(100)    NULL,
    destination_location_id BIGINT UNSIGNED NOT NULL,
    warehouse_id            BIGINT UNSIGNED NULL,
    status                  VARCHAR(32)     NOT NULL DEFAULT 'DRAFT',
    expected_arrival_date   DATE            NULL,
    notes                   VARCHAR(1000)   NULL,
    assigned_receiver_user_id BIGINT UNSIGNED NULL,
    created_by              BIGINT UNSIGNED NOT NULL,
    approved_by             BIGINT UNSIGNED NULL,
    approved_at             TIMESTAMP(6)    NULL,
    verified_by             BIGINT UNSIGNED NULL,
    verified_at             TIMESTAMP(6)    NULL,
    cancelled_by            BIGINT UNSIGNED NULL,
    cancelled_at            TIMESTAMP(6)    NULL,
    created_at              TIMESTAMP(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at              TIMESTAMP(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),
    UNIQUE KEY uk_imports_business_number (business_id, import_number),
    INDEX idx_imports_business (business_id),
    INDEX idx_imports_status (status),
    INDEX idx_imports_destination (destination_location_id),
    INDEX idx_imports_assigned_receiver (assigned_receiver_user_id),
    CONSTRAINT fk_imports_business FOREIGN KEY (business_id) REFERENCES businesses (id),
    CONSTRAINT fk_imports_destination FOREIGN KEY (destination_location_id) REFERENCES locations (id),
    CONSTRAINT fk_imports_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouses (id),
    CONSTRAINT fk_imports_created_by FOREIGN KEY (created_by) REFERENCES users (id),
    CONSTRAINT fk_imports_approved_by FOREIGN KEY (approved_by) REFERENCES users (id),
    CONSTRAINT fk_imports_verified_by FOREIGN KEY (verified_by) REFERENCES users (id),
    CONSTRAINT fk_imports_cancelled_by FOREIGN KEY (cancelled_by) REFERENCES users (id),
    CONSTRAINT fk_imports_assigned_receiver FOREIGN KEY (assigned_receiver_user_id) REFERENCES users (id),
    CONSTRAINT chk_imports_status CHECK (status IN (
        'DRAFT', 'PENDING_APPROVAL', 'APPROVED', 'RECEIVING',
        'PARTIALLY_RECEIVED', 'RECEIVED', 'VERIFIED', 'CANCELLED'
    ))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE import_items (
    id                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    business_id         BIGINT UNSIGNED NOT NULL,
    import_id           BIGINT UNSIGNED NOT NULL,
    product_id          BIGINT UNSIGNED NOT NULL,
    expected_quantity   DECIMAL(19, 4)  NOT NULL,
    received_quantity   DECIMAL(19, 4)  NOT NULL DEFAULT 0.0000,
    unit_cost           DECIMAL(19, 4)  NULL,
    notes               VARCHAR(500)    NULL,
    created_at          TIMESTAMP(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at          TIMESTAMP(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),
    UNIQUE KEY uk_import_items_import_product (import_id, product_id),
    INDEX idx_import_items_business (business_id),
    INDEX idx_import_items_import (import_id),
    INDEX idx_import_items_product (product_id),
    CONSTRAINT fk_import_items_business FOREIGN KEY (business_id) REFERENCES businesses (id),
    CONSTRAINT fk_import_items_import FOREIGN KEY (import_id) REFERENCES imports (id) ON DELETE CASCADE,
    CONSTRAINT fk_import_items_product FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT chk_import_items_expected CHECK (expected_quantity > 0),
    CONSTRAINT chk_import_items_received CHECK (received_quantity >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE import_evidence (
    id                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    business_id         BIGINT UNSIGNED NOT NULL,
    import_id           BIGINT UNSIGNED NOT NULL,
    evidence_type       VARCHAR(32)     NOT NULL,
    description         VARCHAR(1000)   NOT NULL,
    reference_uri       VARCHAR(500)    NULL,
    uploaded_by         BIGINT UNSIGNED NOT NULL,
    created_at          TIMESTAMP(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),
    INDEX idx_import_evidence_business (business_id),
    INDEX idx_import_evidence_import (import_id),
    CONSTRAINT fk_import_evidence_business FOREIGN KEY (business_id) REFERENCES businesses (id),
    CONSTRAINT fk_import_evidence_import FOREIGN KEY (import_id) REFERENCES imports (id) ON DELETE CASCADE,
    CONSTRAINT fk_import_evidence_uploaded_by FOREIGN KEY (uploaded_by) REFERENCES users (id),
    CONSTRAINT chk_import_evidence_type CHECK (evidence_type IN ('PHOTO', 'DOCUMENT', 'NOTE'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Grant import permissions to operational roles (OWNER already has all)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code IN ('import:create', 'import:view', 'import:receive', 'import:verify', 'import:approve')
WHERE r.code = 'GENERAL_MANAGER';

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code IN ('import:create', 'import:view', 'import:receive')
WHERE r.code = 'WAREHOUSE_MANAGER';
