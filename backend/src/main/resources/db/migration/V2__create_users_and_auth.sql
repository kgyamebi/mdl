-- =============================================================================
-- V2: Users and authentication foundation
-- Users are global; business membership links them to tenants (multi-tenant).
-- =============================================================================

CREATE TABLE users (
    id                      BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    email                   VARCHAR(255)    NOT NULL,
    username                VARCHAR(100)    NOT NULL,
    password_hash           VARCHAR(255)    NOT NULL,
    first_name              VARCHAR(100)    NOT NULL,
    last_name               VARCHAR(100)    NOT NULL,
    phone                   VARCHAR(50)     NULL,
    status                  VARCHAR(32)     NOT NULL DEFAULT 'ACTIVE',
    mfa_enabled             BOOLEAN         NOT NULL DEFAULT FALSE,
    last_login_at           TIMESTAMP(6)    NULL,
    password_changed_at     TIMESTAMP(6)    NULL,
    failed_login_attempts   INT             NOT NULL DEFAULT 0,
    locked_until            TIMESTAMP(6)    NULL,
    created_at              TIMESTAMP(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at              TIMESTAMP(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),
    UNIQUE KEY uk_users_email (email),
    UNIQUE KEY uk_users_username (username),
    CONSTRAINT chk_users_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'LOCKED', 'SUSPENDED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE user_business_memberships (
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id         BIGINT UNSIGNED NOT NULL,
    business_id     BIGINT UNSIGNED NOT NULL,
    is_default      BOOLEAN         NOT NULL DEFAULT FALSE,
    status          VARCHAR(32)     NOT NULL DEFAULT 'ACTIVE',
    joined_at       TIMESTAMP(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),
    UNIQUE KEY uk_user_business (user_id, business_id),
    CONSTRAINT fk_membership_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_membership_business FOREIGN KEY (business_id) REFERENCES businesses (id),
    CONSTRAINT chk_membership_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'SUSPENDED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE user_sessions (
    id                      BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id                 BIGINT UNSIGNED NOT NULL,
    business_id             BIGINT UNSIGNED NULL,
    session_token_hash      VARCHAR(255)    NOT NULL,
    refresh_token_hash      VARCHAR(255)    NULL,
    device_info             VARCHAR(500)    NULL,
    ip_address              VARCHAR(45)     NULL,
    user_agent              VARCHAR(500)    NULL,
    expires_at              TIMESTAMP(6)    NOT NULL,
    last_activity_at        TIMESTAMP(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    revoked_at              TIMESTAMP(6)    NULL,
    status                  VARCHAR(32)     NOT NULL DEFAULT 'ACTIVE',
    created_at              TIMESTAMP(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),
    INDEX idx_sessions_user (user_id),
    INDEX idx_sessions_token (session_token_hash),
    INDEX idx_sessions_expires (expires_at),
    CONSTRAINT fk_sessions_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_sessions_business FOREIGN KEY (business_id) REFERENCES businesses (id),
    CONSTRAINT chk_sessions_status CHECK (status IN ('ACTIVE', 'EXPIRED', 'REVOKED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE user_devices (
    id                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id             BIGINT UNSIGNED NOT NULL,
    device_name         VARCHAR(255)    NOT NULL,
    device_fingerprint  VARCHAR(255)    NULL,
    trusted             BOOLEAN         NOT NULL DEFAULT FALSE,
    last_used_at        TIMESTAMP(6)    NULL,
    created_at          TIMESTAMP(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),
    INDEX idx_devices_user (user_id),
    CONSTRAINT fk_devices_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE mfa_credentials (
    id                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id             BIGINT UNSIGNED NOT NULL,
    credential_type     VARCHAR(32)     NOT NULL,
    credential_data     JSON            NOT NULL,
    is_primary          BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at          TIMESTAMP(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),
    INDEX idx_mfa_user (user_id),
    CONSTRAINT fk_mfa_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT chk_mfa_type CHECK (credential_type IN ('TOTP', 'WEBAUTHN', 'BACKUP_CODE'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
