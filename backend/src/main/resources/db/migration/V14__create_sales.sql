-- =============================================================================
-- V14: Sales — POS checkout, payments, inventory deduction via SALE ledger
-- =============================================================================

CREATE TABLE sales (
    id                      BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    business_id             BIGINT UNSIGNED NOT NULL,
    sale_number             VARCHAR(50)     NOT NULL,
    shop_id                 BIGINT UNSIGNED NOT NULL,
    shop_location_id        BIGINT UNSIGNED NOT NULL,
    warehouse_location_id   BIGINT UNSIGNED NOT NULL,
    currency_code           CHAR(3)         NOT NULL,
    status                  VARCHAR(32)     NOT NULL DEFAULT 'COMPLETED',
    subtotal                DECIMAL(19, 4)  NOT NULL,
    total_amount            DECIMAL(19, 4)  NOT NULL,
    customer_name           VARCHAR(255)    NULL,
    notes                   VARCHAR(1000)   NULL,
    sold_by                 BIGINT UNSIGNED NOT NULL,
    cancelled_by            BIGINT UNSIGNED NULL,
    cancelled_at            TIMESTAMP(6)    NULL,
    cancel_reason           VARCHAR(500)    NULL,
    refunded_by             BIGINT UNSIGNED NULL,
    refunded_at             TIMESTAMP(6)    NULL,
    refund_reason           VARCHAR(500)    NULL,
    created_at              TIMESTAMP(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at              TIMESTAMP(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),
    UNIQUE KEY uk_sales_business_number (business_id, sale_number),
    INDEX idx_sales_business (business_id),
    INDEX idx_sales_status (status),
    INDEX idx_sales_shop (shop_id),
    INDEX idx_sales_sold_by (sold_by),
    INDEX idx_sales_created (created_at),
    CONSTRAINT fk_sales_business FOREIGN KEY (business_id) REFERENCES businesses (id),
    CONSTRAINT fk_sales_shop FOREIGN KEY (shop_id) REFERENCES shops (id),
    CONSTRAINT fk_sales_shop_location FOREIGN KEY (shop_location_id) REFERENCES locations (id),
    CONSTRAINT fk_sales_warehouse_location FOREIGN KEY (warehouse_location_id) REFERENCES locations (id),
    CONSTRAINT fk_sales_currency FOREIGN KEY (currency_code) REFERENCES supported_currencies (code),
    CONSTRAINT fk_sales_sold_by FOREIGN KEY (sold_by) REFERENCES users (id),
    CONSTRAINT fk_sales_cancelled_by FOREIGN KEY (cancelled_by) REFERENCES users (id),
    CONSTRAINT fk_sales_refunded_by FOREIGN KEY (refunded_by) REFERENCES users (id),
    CONSTRAINT chk_sales_status CHECK (status IN ('COMPLETED', 'CANCELLED', 'REFUNDED')),
    CONSTRAINT chk_sales_subtotal CHECK (subtotal >= 0),
    CONSTRAINT chk_sales_total CHECK (total_amount >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE sale_items (
    id                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    business_id         BIGINT UNSIGNED NOT NULL,
    sale_id             BIGINT UNSIGNED NOT NULL,
    product_id          BIGINT UNSIGNED NOT NULL,
    quantity            DECIMAL(19, 4)  NOT NULL,
    unit_price          DECIMAL(19, 4)  NOT NULL,
    line_total          DECIMAL(19, 4)  NOT NULL,
    created_at          TIMESTAMP(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at          TIMESTAMP(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),
    INDEX idx_sale_items_business (business_id),
    INDEX idx_sale_items_sale (sale_id),
    INDEX idx_sale_items_product (product_id),
    CONSTRAINT fk_sale_items_business FOREIGN KEY (business_id) REFERENCES businesses (id),
    CONSTRAINT fk_sale_items_sale FOREIGN KEY (sale_id) REFERENCES sales (id) ON DELETE CASCADE,
    CONSTRAINT fk_sale_items_product FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT chk_sale_items_quantity CHECK (quantity > 0),
    CONSTRAINT chk_sale_items_unit_price CHECK (unit_price >= 0),
    CONSTRAINT chk_sale_items_line_total CHECK (line_total >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE sale_payments (
    id                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    business_id         BIGINT UNSIGNED NOT NULL,
    sale_id             BIGINT UNSIGNED NOT NULL,
    payment_method      VARCHAR(32)     NOT NULL,
    amount              DECIMAL(19, 4)  NOT NULL,
    reference           VARCHAR(100)    NULL,
    received_by         BIGINT UNSIGNED NOT NULL,
    created_at          TIMESTAMP(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),
    INDEX idx_sale_payments_business (business_id),
    INDEX idx_sale_payments_sale (sale_id),
    CONSTRAINT fk_sale_payments_business FOREIGN KEY (business_id) REFERENCES businesses (id),
    CONSTRAINT fk_sale_payments_sale FOREIGN KEY (sale_id) REFERENCES sales (id) ON DELETE CASCADE,
    CONSTRAINT fk_sale_payments_received_by FOREIGN KEY (received_by) REFERENCES users (id),
    CONSTRAINT chk_sale_payments_method CHECK (payment_method IN ('CASH', 'MOBILE_MONEY', 'CARD', 'BANK_TRANSFER')),
    CONSTRAINT chk_sale_payments_amount CHECK (amount > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Grant sale permissions to operational roles (OWNER already has all)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code IN ('sale:create', 'sale:view', 'sale:cancel', 'sale:refund')
WHERE r.code IN ('GENERAL_MANAGER', 'SHOP_MANAGER');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code IN ('sale:create', 'sale:view')
WHERE r.code = 'SALES_STAFF';
