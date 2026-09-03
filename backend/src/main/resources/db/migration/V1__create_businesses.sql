-- =============================================================================
-- V1: Core tenant table — businesses (multi-tenant foundation)
-- MDL (Modern Dream Light) will be the first seeded business in a later phase.
-- Currency is per-business and changeable — never hard-coded in application logic.
-- =============================================================================

CREATE TABLE businesses (
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    code            VARCHAR(50)     NOT NULL,
    name            VARCHAR(255)    NOT NULL,
    legal_name      VARCHAR(255)    NULL,
    currency_code   CHAR(3)         NOT NULL DEFAULT 'GHS',
    timezone        VARCHAR(64)     NOT NULL DEFAULT 'Africa/Accra',
    status          VARCHAR(32)     NOT NULL DEFAULT 'ACTIVE',
    settings_json   JSON            NULL,
    created_at      TIMESTAMP(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at      TIMESTAMP(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),
    UNIQUE KEY uk_businesses_code (code),
    CONSTRAINT chk_businesses_status CHECK (status IN ('ACTIVE', 'SUSPENDED', 'CLOSED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ISO 4217 currency reference (expandable — add more currencies as needed)
CREATE TABLE supported_currencies (
    code            CHAR(3)         NOT NULL,
    name            VARCHAR(100)    NOT NULL,
    symbol          VARCHAR(10)     NOT NULL,
    decimal_places  TINYINT         NOT NULL DEFAULT 2,
    is_active       BOOLEAN         NOT NULL DEFAULT TRUE,

    PRIMARY KEY (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO supported_currencies (code, name, symbol, decimal_places) VALUES
    ('GHS', 'Ghana Cedi', 'GHS', 2),
    ('USD', 'US Dollar', '$', 2),
    ('EUR', 'Euro', '€', 2),
    ('GBP', 'British Pound', '£', 2),
    ('NGN', 'Nigerian Naira', '₦', 2);

-- Seed MDL as the initial business (demo data expanded in later phases)
INSERT INTO businesses (code, name, legal_name, currency_code, timezone) VALUES
    ('MDL', 'Modern Dream Light', 'Modern Dream Light', 'GHS', 'Africa/Accra');
