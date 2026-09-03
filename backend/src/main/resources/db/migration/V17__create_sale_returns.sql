-- =============================================================================
-- V17: Customer returns — partial returns against completed sales
-- =============================================================================

ALTER TABLE sale_items
    ADD COLUMN quantity_returned DECIMAL(19, 4) NOT NULL DEFAULT 0 AFTER line_total,
    ADD CONSTRAINT chk_sale_items_returned CHECK (quantity_returned >= 0);

ALTER TABLE sales
    ADD COLUMN returned_amount DECIMAL(19, 4) NOT NULL DEFAULT 0 AFTER total_amount,
    ADD CONSTRAINT chk_sales_returned_amount CHECK (returned_amount >= 0);

ALTER TABLE sales DROP CONSTRAINT chk_sales_status;
ALTER TABLE sales ADD CONSTRAINT chk_sales_status
    CHECK (status IN ('COMPLETED', 'PARTIALLY_RETURNED', 'CANCELLED', 'REFUNDED'));

CREATE TABLE sale_returns (
    id                      BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    business_id             BIGINT UNSIGNED NOT NULL,
    return_number           VARCHAR(50)     NOT NULL,
    sale_id                 BIGINT UNSIGNED NOT NULL,
    shop_id                 BIGINT UNSIGNED NOT NULL,
    warehouse_location_id   BIGINT UNSIGNED NOT NULL,
    currency_code           CHAR(3)         NOT NULL,
    status                  VARCHAR(32)     NOT NULL DEFAULT 'COMPLETED',
    total_refund_amount     DECIMAL(19, 4)  NOT NULL,
    reason                  VARCHAR(32)     NOT NULL,
    notes                   VARCHAR(1000)   NULL,
    processed_by            BIGINT UNSIGNED NOT NULL,
    created_at              TIMESTAMP(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at              TIMESTAMP(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),
    UNIQUE KEY uk_sale_returns_business_number (business_id, return_number),
    INDEX idx_sale_returns_business (business_id),
    INDEX idx_sale_returns_sale (sale_id),
    INDEX idx_sale_returns_shop (shop_id),
    INDEX idx_sale_returns_created (created_at),
    CONSTRAINT fk_sale_returns_business FOREIGN KEY (business_id) REFERENCES businesses (id),
    CONSTRAINT fk_sale_returns_sale FOREIGN KEY (sale_id) REFERENCES sales (id),
    CONSTRAINT fk_sale_returns_shop FOREIGN KEY (shop_id) REFERENCES shops (id),
    CONSTRAINT fk_sale_returns_warehouse_location FOREIGN KEY (warehouse_location_id) REFERENCES locations (id),
    CONSTRAINT fk_sale_returns_currency FOREIGN KEY (currency_code) REFERENCES supported_currencies (code),
    CONSTRAINT fk_sale_returns_processed_by FOREIGN KEY (processed_by) REFERENCES users (id),
    CONSTRAINT chk_sale_returns_status CHECK (status IN ('COMPLETED')),
    CONSTRAINT chk_sale_returns_reason CHECK (reason IN (
        'DEFECTIVE', 'WRONG_ITEM', 'CUSTOMER_CHANGED_MIND', 'OTHER'
    )),
    CONSTRAINT chk_sale_returns_refund CHECK (total_refund_amount > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE sale_return_items (
    id                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    business_id         BIGINT UNSIGNED NOT NULL,
    sale_return_id      BIGINT UNSIGNED NOT NULL,
    sale_item_id        BIGINT UNSIGNED NOT NULL,
    product_id          BIGINT UNSIGNED NOT NULL,
    quantity            DECIMAL(19, 4)  NOT NULL,
    unit_price          DECIMAL(19, 4)  NOT NULL,
    line_refund         DECIMAL(19, 4)  NOT NULL,
    created_at          TIMESTAMP(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),
    INDEX idx_sale_return_items_business (business_id),
    INDEX idx_sale_return_items_return (sale_return_id),
    INDEX idx_sale_return_items_sale_item (sale_item_id),
    CONSTRAINT fk_sale_return_items_business FOREIGN KEY (business_id) REFERENCES businesses (id),
    CONSTRAINT fk_sale_return_items_return FOREIGN KEY (sale_return_id) REFERENCES sale_returns (id) ON DELETE CASCADE,
    CONSTRAINT fk_sale_return_items_sale_item FOREIGN KEY (sale_item_id) REFERENCES sale_items (id),
    CONSTRAINT fk_sale_return_items_product FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT chk_sale_return_items_quantity CHECK (quantity > 0),
    CONSTRAINT chk_sale_return_items_unit_price CHECK (unit_price >= 0),
    CONSTRAINT chk_sale_return_items_line_refund CHECK (line_refund > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE sale_return_refunds (
    id                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    business_id         BIGINT UNSIGNED NOT NULL,
    sale_return_id      BIGINT UNSIGNED NOT NULL,
    payment_method      VARCHAR(32)     NOT NULL,
    amount              DECIMAL(19, 4)  NOT NULL,
    reference           VARCHAR(100)    NULL,
    processed_by        BIGINT UNSIGNED NOT NULL,
    created_at          TIMESTAMP(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),
    INDEX idx_sale_return_refunds_business (business_id),
    INDEX idx_sale_return_refunds_return (sale_return_id),
    CONSTRAINT fk_sale_return_refunds_business FOREIGN KEY (business_id) REFERENCES businesses (id),
    CONSTRAINT fk_sale_return_refunds_return FOREIGN KEY (sale_return_id) REFERENCES sale_returns (id) ON DELETE CASCADE,
    CONSTRAINT fk_sale_return_refunds_processed_by FOREIGN KEY (processed_by) REFERENCES users (id),
    CONSTRAINT chk_sale_return_refunds_method CHECK (payment_method IN (
        'CASH', 'MOBILE_MONEY', 'CARD', 'BANK_TRANSFER'
    )),
    CONSTRAINT chk_sale_return_refunds_amount CHECK (amount > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO permissions (code, name, module) VALUES
    ('sale:return', 'Process customer returns', 'sales');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code = 'sale:return'
WHERE r.code IN ('GENERAL_MANAGER', 'SHOP_MANAGER');
