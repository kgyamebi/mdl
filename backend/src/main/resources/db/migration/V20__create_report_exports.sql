-- =============================================================================
-- V20: Report exports — CSV downloads with export audit trail
-- =============================================================================

CREATE TABLE report_exports (
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    business_id     BIGINT UNSIGNED NOT NULL,
    user_id         BIGINT UNSIGNED NOT NULL,
    report_type     VARCHAR(64)     NOT NULL,
    export_format   VARCHAR(16)     NOT NULL DEFAULT 'CSV',
    file_name       VARCHAR(255)    NOT NULL,
    row_count       INT UNSIGNED    NOT NULL DEFAULT 0,
    parameters      JSON            NULL,
    status          VARCHAR(16)     NOT NULL DEFAULT 'COMPLETED',
    created_at      TIMESTAMP(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),
    INDEX idx_report_exports_business (business_id),
    INDEX idx_report_exports_user (user_id),
    INDEX idx_report_exports_type (report_type),
    INDEX idx_report_exports_created (created_at),
    CONSTRAINT fk_report_exports_business FOREIGN KEY (business_id) REFERENCES businesses (id),
    CONSTRAINT fk_report_exports_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT chk_report_exports_format CHECK (export_format IN ('CSV')),
    CONSTRAINT chk_report_exports_status CHECK (status IN ('COMPLETED', 'FAILED')),
    CONSTRAINT chk_report_exports_type CHECK (report_type IN (
        'SALES_SUMMARY', 'INVENTORY_BALANCES', 'LOW_STOCK'
    ))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
