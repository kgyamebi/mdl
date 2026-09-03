-- =============================================================================
-- V19: Stocktakes — physical stock counts with variance approval
-- =============================================================================

CREATE TABLE stocktakes (
    id                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    business_id         BIGINT UNSIGNED NOT NULL,
    stocktake_number    VARCHAR(50)     NOT NULL,
    location_id         BIGINT UNSIGNED NOT NULL,
    status              VARCHAR(32)     NOT NULL DEFAULT 'IN_PROGRESS',
    notes               VARCHAR(1000)   NULL,
    line_count          INT UNSIGNED    NOT NULL DEFAULT 0,
    variance_line_count INT UNSIGNED    NOT NULL DEFAULT 0,
    total_variance      DECIMAL(19, 4)  NOT NULL DEFAULT 0,
    started_by          BIGINT UNSIGNED NOT NULL,
    submitted_by        BIGINT UNSIGNED NULL,
    submitted_at        TIMESTAMP(6)    NULL,
    approved_by         BIGINT UNSIGNED NULL,
    approved_at         TIMESTAMP(6)    NULL,
    cancelled_by        BIGINT UNSIGNED NULL,
    cancelled_at        TIMESTAMP(6)    NULL,
    cancel_reason       VARCHAR(500)    NULL,
    created_at          TIMESTAMP(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at          TIMESTAMP(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),
    UNIQUE KEY uk_stocktakes_business_number (business_id, stocktake_number),
    INDEX idx_stocktakes_business (business_id),
    INDEX idx_stocktakes_location (location_id),
    INDEX idx_stocktakes_status (status),
    INDEX idx_stocktakes_created (created_at),
    CONSTRAINT fk_stocktakes_business FOREIGN KEY (business_id) REFERENCES businesses (id),
    CONSTRAINT fk_stocktakes_location FOREIGN KEY (location_id) REFERENCES locations (id),
    CONSTRAINT fk_stocktakes_started_by FOREIGN KEY (started_by) REFERENCES users (id),
    CONSTRAINT fk_stocktakes_submitted_by FOREIGN KEY (submitted_by) REFERENCES users (id),
    CONSTRAINT fk_stocktakes_approved_by FOREIGN KEY (approved_by) REFERENCES users (id),
    CONSTRAINT fk_stocktakes_cancelled_by FOREIGN KEY (cancelled_by) REFERENCES users (id),
    CONSTRAINT chk_stocktakes_status CHECK (status IN (
        'IN_PROGRESS', 'SUBMITTED', 'COMPLETED', 'CANCELLED'
    ))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE stocktake_lines (
    id                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    business_id         BIGINT UNSIGNED NOT NULL,
    stocktake_id        BIGINT UNSIGNED NOT NULL,
    product_id          BIGINT UNSIGNED NOT NULL,
    expected_quantity   DECIMAL(19, 4)  NOT NULL,
    counted_quantity    DECIMAL(19, 4)  NULL,
    variance            DECIMAL(19, 4)  NULL,
    notes               VARCHAR(500)    NULL,
    result_transaction_id BIGINT UNSIGNED NULL,
    created_at          TIMESTAMP(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at          TIMESTAMP(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),
    UNIQUE KEY uk_stocktake_lines_product (stocktake_id, product_id),
    INDEX idx_stocktake_lines_business (business_id),
    INDEX idx_stocktake_lines_stocktake (stocktake_id),
    INDEX idx_stocktake_lines_product (product_id),
    CONSTRAINT fk_stocktake_lines_business FOREIGN KEY (business_id) REFERENCES businesses (id),
    CONSTRAINT fk_stocktake_lines_stocktake FOREIGN KEY (stocktake_id) REFERENCES stocktakes (id) ON DELETE CASCADE,
    CONSTRAINT fk_stocktake_lines_product FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT fk_stocktake_lines_result_txn FOREIGN KEY (result_transaction_id) REFERENCES inventory_transactions (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code = 'stock:count'
WHERE r.code IN ('GENERAL_MANAGER', 'WAREHOUSE_MANAGER', 'SHOP_MANAGER', 'SHOP_WORKER');
