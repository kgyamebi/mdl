-- =============================================================================
-- V18: In-app notifications — user inbox for alerts and workflow events
-- =============================================================================

CREATE TABLE notifications (
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    business_id     BIGINT UNSIGNED NOT NULL,
    user_id         BIGINT UNSIGNED NOT NULL,
    notification_type VARCHAR(64)   NOT NULL,
    category        VARCHAR(32)     NOT NULL,
    title           VARCHAR(255)    NOT NULL,
    message         VARCHAR(1000)   NOT NULL,
    entity_type     VARCHAR(64)     NULL,
    entity_id       BIGINT UNSIGNED NULL,
    entity_ref      VARCHAR(100)    NULL,
    source_type     VARCHAR(64)     NULL,
    source_id       BIGINT UNSIGNED NULL,
    dedupe_key      VARCHAR(255)    NULL,
    status          VARCHAR(16)     NOT NULL DEFAULT 'UNREAD',
    read_at         TIMESTAMP(6)    NULL,
    dismissed_at    TIMESTAMP(6)    NULL,
    created_at      TIMESTAMP(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),
    INDEX idx_notifications_business (business_id),
    INDEX idx_notifications_user (user_id),
    INDEX idx_notifications_status (status),
    INDEX idx_notifications_category (category),
    INDEX idx_notifications_dedupe (business_id, user_id, dedupe_key),
    INDEX idx_notifications_created (created_at),
    CONSTRAINT fk_notifications_business FOREIGN KEY (business_id) REFERENCES businesses (id),
    CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT chk_notifications_category CHECK (category IN (
        'ALERT', 'SECURITY', 'APPROVAL', 'SYSTEM'
    )),
    CONSTRAINT chk_notifications_status CHECK (status IN ('UNREAD', 'READ', 'DISMISSED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
