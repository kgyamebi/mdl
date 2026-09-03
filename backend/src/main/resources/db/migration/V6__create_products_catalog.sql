-- =============================================================================
-- V6: Product catalog — categories, products, barcodes
-- Inventory quantities live in inventory_balances (Phase 7+) — NOT on products.
-- =============================================================================

CREATE TABLE product_categories (
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    business_id     BIGINT UNSIGNED NOT NULL,
    parent_id       BIGINT UNSIGNED NULL,
    name            VARCHAR(255)    NOT NULL,
    code            VARCHAR(50)     NOT NULL,
    description     VARCHAR(500)    NULL,
    sort_order      INT             NOT NULL DEFAULT 0,
    status          VARCHAR(32)     NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMP(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at      TIMESTAMP(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),
    UNIQUE KEY uk_product_categories_business_code (business_id, code),
    INDEX idx_product_categories_business (business_id),
    INDEX idx_product_categories_parent (parent_id),
    CONSTRAINT fk_product_categories_business FOREIGN KEY (business_id) REFERENCES businesses (id),
    CONSTRAINT fk_product_categories_parent FOREIGN KEY (parent_id) REFERENCES product_categories (id),
    CONSTRAINT chk_product_categories_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE products (
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    business_id     BIGINT UNSIGNED NOT NULL,
    category_id     BIGINT UNSIGNED NULL,
    sku             VARCHAR(64)     NOT NULL,
    name            VARCHAR(255)    NOT NULL,
    description     TEXT            NULL,
    brand           VARCHAR(100)    NULL,
    unit_of_measure VARCHAR(32)     NOT NULL DEFAULT 'PIECE',
    cost_price      DECIMAL(19, 4)  NULL,
    selling_price   DECIMAL(19, 4)  NOT NULL,
    tax_inclusive   BOOLEAN         NOT NULL DEFAULT TRUE,
    track_inventory BOOLEAN         NOT NULL DEFAULT TRUE,
    reorder_level   INT UNSIGNED    NULL,
    status          VARCHAR(32)     NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMP(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at      TIMESTAMP(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),
    UNIQUE KEY uk_products_business_sku (business_id, sku),
    INDEX idx_products_business (business_id),
    INDEX idx_products_category (category_id),
    INDEX idx_products_status (status),
    INDEX idx_products_name (name),
    CONSTRAINT fk_products_business FOREIGN KEY (business_id) REFERENCES businesses (id),
    CONSTRAINT fk_products_category FOREIGN KEY (category_id) REFERENCES product_categories (id),
    CONSTRAINT chk_products_status CHECK (status IN ('ACTIVE', 'DISCONTINUED', 'DRAFT')),
    CONSTRAINT chk_products_unit CHECK (unit_of_measure IN ('PIECE', 'METRE', 'BOX', 'ROLL', 'PACK', 'SET'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE barcodes (
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    business_id     BIGINT UNSIGNED NOT NULL,
    product_id      BIGINT UNSIGNED NOT NULL,
    barcode         VARCHAR(64)     NOT NULL,
    barcode_type    VARCHAR(32)     NOT NULL DEFAULT 'EAN13',
    is_primary      BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at      TIMESTAMP(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),
    UNIQUE KEY uk_barcodes_business_barcode (business_id, barcode),
    INDEX idx_barcodes_product (product_id),
    INDEX idx_barcodes_business (business_id),
    CONSTRAINT fk_barcodes_business FOREIGN KEY (business_id) REFERENCES businesses (id),
    CONSTRAINT fk_barcodes_product FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE CASCADE,
    CONSTRAINT chk_barcodes_type CHECK (barcode_type IN ('EAN13', 'UPC', 'CODE128', 'INTERNAL', 'QR'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------------
-- Product catalog permissions
-- ---------------------------------------------------------------------------

INSERT INTO permissions (code, name, module) VALUES
    ('product:view',   'View product catalog',           'products'),
    ('product:manage', 'Manage products and categories', 'products');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code IN ('product:view', 'product:manage')
WHERE r.code = 'OWNER';

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code = 'product:view'
WHERE r.code IN ('VIEWER', 'SHOP_WORKER', 'SALES_STAFF', 'ACCOUNTANT', 'AUDITOR', 'IMPORT_RECEIVING_STAFF');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code IN ('product:view', 'product:manage')
WHERE r.code IN ('SHOP_MANAGER', 'GENERAL_MANAGER', 'WAREHOUSE_MANAGER');
