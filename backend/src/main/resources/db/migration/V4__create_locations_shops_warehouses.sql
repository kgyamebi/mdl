-- =============================================================================
-- V4: Locations, shops, warehouses, and location-based access
-- Supports multiple MAIN warehouses per business (warehouse_type is data, not code).
-- =============================================================================

CREATE TABLE locations (
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    business_id     BIGINT UNSIGNED NOT NULL,
    name            VARCHAR(255)    NOT NULL,
    code            VARCHAR(50)     NOT NULL,
    location_type   VARCHAR(32)     NOT NULL,
    address_line1   VARCHAR(255)    NULL,
    address_line2   VARCHAR(255)    NULL,
    city            VARCHAR(100)    NULL,
    region          VARCHAR(100)    NULL,
    country         VARCHAR(100)    NULL,
    postal_code     VARCHAR(20)     NULL,
    latitude        DECIMAL(10, 7)  NULL,
    longitude       DECIMAL(10, 7)  NULL,
    phone           VARCHAR(50)     NULL,
    status          VARCHAR(32)     NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMP(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at      TIMESTAMP(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),
    UNIQUE KEY uk_locations_business_code (business_id, code),
    INDEX idx_locations_business (business_id),
    INDEX idx_locations_type (location_type),
    CONSTRAINT fk_locations_business FOREIGN KEY (business_id) REFERENCES businesses (id),
    CONSTRAINT chk_locations_type CHECK (location_type IN ('SHOP', 'WAREHOUSE', 'OFFICE', 'TRANSIT')),
    CONSTRAINT chk_locations_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'CLOSED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE warehouses (
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    business_id     BIGINT UNSIGNED NOT NULL,
    location_id     BIGINT UNSIGNED NOT NULL,
    name            VARCHAR(255)    NOT NULL,
    code            VARCHAR(50)     NOT NULL,
    warehouse_type  VARCHAR(32)     NOT NULL,
    is_restricted   BOOLEAN         NOT NULL DEFAULT FALSE,
    description     VARCHAR(500)    NULL,
    status          VARCHAR(32)     NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMP(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at      TIMESTAMP(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),
    UNIQUE KEY uk_warehouses_business_code (business_id, code),
    INDEX idx_warehouses_business (business_id),
    INDEX idx_warehouses_type (warehouse_type),
    INDEX idx_warehouses_location (location_id),
    CONSTRAINT fk_warehouses_business FOREIGN KEY (business_id) REFERENCES businesses (id),
    CONSTRAINT fk_warehouses_location FOREIGN KEY (location_id) REFERENCES locations (id),
    CONSTRAINT chk_warehouses_type CHECK (
        warehouse_type IN ('MAIN', 'SHOP', 'REGIONAL', 'DISTRIBUTION', 'TEMPORARY', 'TRANSIT')
    ),
    CONSTRAINT chk_warehouses_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'CLOSED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE shops (
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    business_id     BIGINT UNSIGNED NOT NULL,
    location_id     BIGINT UNSIGNED NOT NULL,
    warehouse_id    BIGINT UNSIGNED NULL,
    name            VARCHAR(255)    NOT NULL,
    code            VARCHAR(50)     NOT NULL,
    status          VARCHAR(32)     NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMP(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at      TIMESTAMP(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),
    UNIQUE KEY uk_shops_business_code (business_id, code),
    INDEX idx_shops_business (business_id),
    INDEX idx_shops_location (location_id),
    CONSTRAINT fk_shops_business FOREIGN KEY (business_id) REFERENCES businesses (id),
    CONSTRAINT fk_shops_location FOREIGN KEY (location_id) REFERENCES locations (id),
    CONSTRAINT fk_shops_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouses (id),
    CONSTRAINT chk_shops_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'CLOSED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE warehouse_transfer_routes (
    id                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    business_id         BIGINT UNSIGNED NOT NULL,
    from_warehouse_id   BIGINT UNSIGNED NOT NULL,
    to_warehouse_id     BIGINT UNSIGNED NOT NULL,
    enabled             BOOLEAN         NOT NULL DEFAULT TRUE,
    notes               VARCHAR(500)    NULL,
    created_at          TIMESTAMP(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),
    UNIQUE KEY uk_transfer_route (business_id, from_warehouse_id, to_warehouse_id),
    INDEX idx_routes_from (from_warehouse_id),
    INDEX idx_routes_to (to_warehouse_id),
    CONSTRAINT fk_routes_business FOREIGN KEY (business_id) REFERENCES businesses (id),
    CONSTRAINT fk_routes_from FOREIGN KEY (from_warehouse_id) REFERENCES warehouses (id),
    CONSTRAINT fk_routes_to FOREIGN KEY (to_warehouse_id) REFERENCES warehouses (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE user_location_assignments (
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id         BIGINT UNSIGNED NOT NULL,
    business_id     BIGINT UNSIGNED NOT NULL,
    location_id     BIGINT UNSIGNED NOT NULL,
    access_level    VARCHAR(32)     NOT NULL DEFAULT 'FULL',
    assigned_at     TIMESTAMP(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    assigned_by     BIGINT UNSIGNED NULL,

    PRIMARY KEY (id),
    UNIQUE KEY uk_user_location (user_id, location_id),
    INDEX idx_ula_business (business_id),
    INDEX idx_ula_location (location_id),
    CONSTRAINT fk_ula_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_ula_business FOREIGN KEY (business_id) REFERENCES businesses (id),
    CONSTRAINT fk_ula_location FOREIGN KEY (location_id) REFERENCES locations (id),
    CONSTRAINT fk_ula_assigned_by FOREIGN KEY (assigned_by) REFERENCES users (id),
    CONSTRAINT chk_ula_access CHECK (access_level IN ('FULL', 'READ_ONLY', 'NONE'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Link business currency to supported currencies (referential integrity)
ALTER TABLE businesses
    ADD CONSTRAINT fk_businesses_currency
    FOREIGN KEY (currency_code) REFERENCES supported_currencies (code);
