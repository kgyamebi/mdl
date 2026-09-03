-- =============================================================================
-- V13: Stock transfers — shop requests and warehouse-to-warehouse distribution
-- Uses authorized routes, reservations on approve, TRANSFER_OUT/IN ledger entries.
-- =============================================================================

CREATE TABLE stock_transfers (
    id                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    business_id         BIGINT UNSIGNED NOT NULL,
    transfer_number     VARCHAR(50)     NOT NULL,
    from_warehouse_id   BIGINT UNSIGNED NOT NULL,
    to_warehouse_id     BIGINT UNSIGNED NOT NULL,
    from_location_id    BIGINT UNSIGNED NOT NULL,
    to_location_id      BIGINT UNSIGNED NOT NULL,
    status              VARCHAR(32)     NOT NULL DEFAULT 'REQUESTED',
    notes               VARCHAR(1000)   NULL,
    requested_by        BIGINT UNSIGNED NOT NULL,
    approved_by         BIGINT UNSIGNED NULL,
    approved_at         TIMESTAMP(6)    NULL,
    dispatched_by       BIGINT UNSIGNED NULL,
    dispatched_at       TIMESTAMP(6)    NULL,
    rejected_by         BIGINT UNSIGNED NULL,
    rejected_at         TIMESTAMP(6)    NULL,
    reject_reason       VARCHAR(500)    NULL,
    cancelled_by        BIGINT UNSIGNED NULL,
    cancelled_at        TIMESTAMP(6)    NULL,
    created_at          TIMESTAMP(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at          TIMESTAMP(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),
    UNIQUE KEY uk_stock_transfers_business_number (business_id, transfer_number),
    INDEX idx_stock_transfers_business (business_id),
    INDEX idx_stock_transfers_status (status),
    INDEX idx_stock_transfers_from_wh (from_warehouse_id),
    INDEX idx_stock_transfers_to_wh (to_warehouse_id),
    CONSTRAINT fk_stock_transfers_business FOREIGN KEY (business_id) REFERENCES businesses (id),
    CONSTRAINT fk_stock_transfers_from_wh FOREIGN KEY (from_warehouse_id) REFERENCES warehouses (id),
    CONSTRAINT fk_stock_transfers_to_wh FOREIGN KEY (to_warehouse_id) REFERENCES warehouses (id),
    CONSTRAINT fk_stock_transfers_from_loc FOREIGN KEY (from_location_id) REFERENCES locations (id),
    CONSTRAINT fk_stock_transfers_to_loc FOREIGN KEY (to_location_id) REFERENCES locations (id),
    CONSTRAINT fk_stock_transfers_requested_by FOREIGN KEY (requested_by) REFERENCES users (id),
    CONSTRAINT fk_stock_transfers_approved_by FOREIGN KEY (approved_by) REFERENCES users (id),
    CONSTRAINT fk_stock_transfers_dispatched_by FOREIGN KEY (dispatched_by) REFERENCES users (id),
    CONSTRAINT fk_stock_transfers_rejected_by FOREIGN KEY (rejected_by) REFERENCES users (id),
    CONSTRAINT fk_stock_transfers_cancelled_by FOREIGN KEY (cancelled_by) REFERENCES users (id),
    CONSTRAINT chk_stock_transfers_status CHECK (status IN (
        'REQUESTED', 'APPROVED', 'DISPATCHED', 'PARTIALLY_RECEIVED', 'RECEIVED', 'REJECTED', 'CANCELLED'
    ))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE stock_transfer_items (
    id                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    business_id         BIGINT UNSIGNED NOT NULL,
    transfer_id         BIGINT UNSIGNED NOT NULL,
    product_id          BIGINT UNSIGNED NOT NULL,
    requested_quantity  DECIMAL(19, 4)  NOT NULL,
    dispatched_quantity DECIMAL(19, 4)  NOT NULL DEFAULT 0.0000,
    received_quantity   DECIMAL(19, 4)  NOT NULL DEFAULT 0.0000,
    notes               VARCHAR(500)    NULL,
    created_at          TIMESTAMP(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at          TIMESTAMP(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),
    UNIQUE KEY uk_stock_transfer_items_transfer_product (transfer_id, product_id),
    INDEX idx_stock_transfer_items_business (business_id),
    INDEX idx_stock_transfer_items_transfer (transfer_id),
    INDEX idx_stock_transfer_items_product (product_id),
    CONSTRAINT fk_stock_transfer_items_business FOREIGN KEY (business_id) REFERENCES businesses (id),
    CONSTRAINT fk_stock_transfer_items_transfer FOREIGN KEY (transfer_id) REFERENCES stock_transfers (id) ON DELETE CASCADE,
    CONSTRAINT fk_stock_transfer_items_product FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT chk_stock_transfer_items_requested CHECK (requested_quantity > 0),
    CONSTRAINT chk_stock_transfer_items_dispatched CHECK (dispatched_quantity >= 0),
    CONSTRAINT chk_stock_transfer_items_received CHECK (received_quantity >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Grant transfer permissions to operational roles (OWNER already has all)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code IN (
    'transfer:create', 'transfer:view', 'transfer:approve',
    'transfer:dispatch', 'transfer:receive', 'stock:request'
)
WHERE r.code = 'GENERAL_MANAGER';

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code IN (
    'transfer:create', 'transfer:view', 'transfer:approve',
    'transfer:dispatch', 'transfer:receive'
)
WHERE r.code = 'WAREHOUSE_MANAGER';

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code IN (
    'transfer:view', 'transfer:approve', 'transfer:receive', 'stock:request'
)
WHERE r.code = 'SHOP_MANAGER';
