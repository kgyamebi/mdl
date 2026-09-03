-- =============================================================================
-- V10: Inventory workflows — adjustment requests, reservations, constraints
-- Phase 8 extends the ledger with approval flows and stock holds.
-- =============================================================================

CREATE TABLE inventory_adjustment_requests (
    id                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    business_id         BIGINT UNSIGNED NOT NULL,
    location_id         BIGINT UNSIGNED NOT NULL,
    product_id          BIGINT UNSIGNED NOT NULL,
    requested_change    DECIMAL(19, 4)  NOT NULL,
    reason              VARCHAR(500)    NOT NULL,
    status              VARCHAR(32)     NOT NULL DEFAULT 'PENDING',
    requested_by        BIGINT UNSIGNED NOT NULL,
    reviewed_by         BIGINT UNSIGNED NULL,
    reviewed_at         TIMESTAMP(6)    NULL,
    review_notes        VARCHAR(500)    NULL,
    result_transaction_id BIGINT UNSIGNED NULL,
    created_at          TIMESTAMP(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at          TIMESTAMP(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),
    INDEX idx_adj_requests_business (business_id),
    INDEX idx_adj_requests_status (status),
    INDEX idx_adj_requests_location (location_id),
    INDEX idx_adj_requests_product (product_id),
    CONSTRAINT fk_adj_requests_business FOREIGN KEY (business_id) REFERENCES businesses (id),
    CONSTRAINT fk_adj_requests_location FOREIGN KEY (location_id) REFERENCES locations (id),
    CONSTRAINT fk_adj_requests_product FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT fk_adj_requests_requested_by FOREIGN KEY (requested_by) REFERENCES users (id),
    CONSTRAINT fk_adj_requests_reviewed_by FOREIGN KEY (reviewed_by) REFERENCES users (id),
    CONSTRAINT fk_adj_requests_result_txn FOREIGN KEY (result_transaction_id) REFERENCES inventory_transactions (id),
    CONSTRAINT chk_adj_requests_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'CANCELLED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE inventory_reservations (
    id                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    business_id         BIGINT UNSIGNED NOT NULL,
    location_id         BIGINT UNSIGNED NOT NULL,
    product_id          BIGINT UNSIGNED NOT NULL,
    quantity            DECIMAL(19, 4)  NOT NULL,
    reference_type      VARCHAR(32)     NOT NULL DEFAULT 'MANUAL',
    reference_id        BIGINT UNSIGNED NULL,
    status              VARCHAR(32)     NOT NULL DEFAULT 'ACTIVE',
    notes               VARCHAR(500)    NULL,
    reserved_by         BIGINT UNSIGNED NOT NULL,
    released_by         BIGINT UNSIGNED NULL,
    released_at         TIMESTAMP(6)    NULL,
    created_at          TIMESTAMP(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at          TIMESTAMP(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),
    INDEX idx_reservations_business (business_id),
    INDEX idx_reservations_status (status),
    INDEX idx_reservations_location (location_id),
    INDEX idx_reservations_product (product_id),
    INDEX idx_reservations_reference (reference_type, reference_id),
    CONSTRAINT fk_reservations_business FOREIGN KEY (business_id) REFERENCES businesses (id),
    CONSTRAINT fk_reservations_location FOREIGN KEY (location_id) REFERENCES locations (id),
    CONSTRAINT fk_reservations_product FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT fk_reservations_reserved_by FOREIGN KEY (reserved_by) REFERENCES users (id),
    CONSTRAINT fk_reservations_released_by FOREIGN KEY (released_by) REFERENCES users (id),
    CONSTRAINT chk_reservations_status CHECK (status IN ('ACTIVE', 'RELEASED', 'CONSUMED')),
    CONSTRAINT chk_reservations_quantity CHECK (quantity > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE inventory_balances
    ADD CONSTRAINT chk_inventory_balances_reserved_lte_on_hand
    CHECK (quantity_reserved <= quantity_on_hand);

-- ---------------------------------------------------------------------------
-- Reservation permission + shop manager can approve adjustments
-- ---------------------------------------------------------------------------

INSERT INTO permissions (code, name, module) VALUES
    ('inventory:reserve', 'Reserve inventory stock', 'inventory');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code = 'inventory:reserve'
WHERE r.code = 'OWNER';

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code = 'inventory:reserve'
WHERE r.code IN ('GENERAL_MANAGER', 'WAREHOUSE_MANAGER', 'SHOP_MANAGER');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code = 'inventory:adjust'
WHERE r.code = 'SHOP_MANAGER';
