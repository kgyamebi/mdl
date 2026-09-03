-- =============================================================================
-- V8: Inventory balances + immutable transaction ledger
-- Balances are derived from transactions — never update quantity without a ledger row.
-- =============================================================================

CREATE TABLE inventory_transactions (
    id                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    business_id         BIGINT UNSIGNED NOT NULL,
    location_id         BIGINT UNSIGNED NOT NULL,
    product_id          BIGINT UNSIGNED NOT NULL,
    transaction_type    VARCHAR(32)     NOT NULL,
    quantity_change     DECIMAL(19, 4)  NOT NULL,
    quantity_after      DECIMAL(19, 4)  NOT NULL,
    reference_type      VARCHAR(32)     NULL,
    reference_id        BIGINT UNSIGNED NULL,
    notes               VARCHAR(500)    NULL,
    performed_by        BIGINT UNSIGNED NULL,
    transaction_at      TIMESTAMP(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    created_at          TIMESTAMP(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),
    INDEX idx_inventory_txn_business (business_id),
    INDEX idx_inventory_txn_location (location_id),
    INDEX idx_inventory_txn_product (product_id),
    INDEX idx_inventory_txn_type (transaction_type),
    INDEX idx_inventory_txn_at (transaction_at),
    INDEX idx_inventory_txn_reference (reference_type, reference_id),
    CONSTRAINT fk_inventory_txn_business FOREIGN KEY (business_id) REFERENCES businesses (id),
    CONSTRAINT fk_inventory_txn_location FOREIGN KEY (location_id) REFERENCES locations (id),
    CONSTRAINT fk_inventory_txn_product FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT fk_inventory_txn_performed_by FOREIGN KEY (performed_by) REFERENCES users (id),
    CONSTRAINT chk_inventory_txn_type CHECK (transaction_type IN (
        'OPENING_BALANCE', 'ADJUSTMENT', 'IMPORT_RECEIVE', 'TRANSFER_OUT', 'TRANSFER_IN',
        'SALE', 'SALE_CANCEL', 'RETURN', 'DAMAGE', 'STOCKTAKE'
    ))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE inventory_balances (
    id                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    business_id         BIGINT UNSIGNED NOT NULL,
    location_id         BIGINT UNSIGNED NOT NULL,
    product_id          BIGINT UNSIGNED NOT NULL,
    quantity_on_hand    DECIMAL(19, 4)  NOT NULL DEFAULT 0.0000,
    quantity_reserved   DECIMAL(19, 4)  NOT NULL DEFAULT 0.0000,
    last_transaction_id BIGINT UNSIGNED NULL,
    created_at          TIMESTAMP(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at          TIMESTAMP(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),
    UNIQUE KEY uk_inventory_balances_location_product (business_id, location_id, product_id),
    INDEX idx_inventory_balances_business (business_id),
    INDEX idx_inventory_balances_location (location_id),
    INDEX idx_inventory_balances_product (product_id),
    CONSTRAINT fk_inventory_balances_business FOREIGN KEY (business_id) REFERENCES businesses (id),
    CONSTRAINT fk_inventory_balances_location FOREIGN KEY (location_id) REFERENCES locations (id),
    CONSTRAINT fk_inventory_balances_product FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT fk_inventory_balances_last_txn FOREIGN KEY (last_transaction_id) REFERENCES inventory_transactions (id),
    CONSTRAINT chk_inventory_balances_non_negative CHECK (quantity_on_hand >= 0 AND quantity_reserved >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------------
-- Direct adjustment permission (opening stock / corrections by authorized staff)
-- ---------------------------------------------------------------------------

INSERT INTO permissions (code, name, module) VALUES
    ('inventory:adjust', 'Post inventory adjustments', 'inventory');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code = 'inventory:adjust'
WHERE r.code = 'OWNER';

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code = 'inventory:adjust'
WHERE r.code IN ('GENERAL_MANAGER', 'WAREHOUSE_MANAGER');
