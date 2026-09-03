-- =============================================================================
-- V24: AI Copilot — conversations, messages, usage logging
-- Phase 35
-- =============================================================================

CREATE TABLE copilot_conversations (
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    business_id     BIGINT UNSIGNED NOT NULL,
    user_id         BIGINT UNSIGNED NOT NULL,
    title           VARCHAR(200)    NULL,
    created_at      TIMESTAMP(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at      TIMESTAMP(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),
    INDEX idx_copilot_conversations_business (business_id),
    INDEX idx_copilot_conversations_user (user_id),
    INDEX idx_copilot_conversations_updated (updated_at),
    CONSTRAINT fk_copilot_conversations_business FOREIGN KEY (business_id) REFERENCES businesses (id),
    CONSTRAINT fk_copilot_conversations_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE copilot_messages (
    id                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    conversation_id     BIGINT UNSIGNED NOT NULL,
    role                VARCHAR(16)     NOT NULL,
    content             TEXT            NOT NULL,
    prompt_tokens       INT UNSIGNED    NOT NULL DEFAULT 0,
    completion_tokens   INT UNSIGNED    NOT NULL DEFAULT 0,
    provider            VARCHAR(32)     NOT NULL DEFAULT 'data-grounded',
    model               VARCHAR(64)     NULL,
    created_at          TIMESTAMP(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),
    INDEX idx_copilot_messages_conversation (conversation_id),
    INDEX idx_copilot_messages_created (created_at),
    CONSTRAINT fk_copilot_messages_conversation FOREIGN KEY (conversation_id) REFERENCES copilot_conversations (id),
    CONSTRAINT chk_copilot_messages_role CHECK (role IN ('USER', 'ASSISTANT'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE copilot_usage_logs (
    id                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    business_id         BIGINT UNSIGNED NOT NULL,
    user_id             BIGINT UNSIGNED NOT NULL,
    conversation_id     BIGINT UNSIGNED NULL,
    message_id          BIGINT UNSIGNED NULL,
    provider            VARCHAR(32)     NOT NULL,
    model               VARCHAR(64)     NULL,
    prompt_tokens       INT UNSIGNED    NOT NULL DEFAULT 0,
    completion_tokens   INT UNSIGNED    NOT NULL DEFAULT 0,
    created_at          TIMESTAMP(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),
    INDEX idx_copilot_usage_business (business_id),
    INDEX idx_copilot_usage_user (user_id),
    INDEX idx_copilot_usage_created (created_at),
    CONSTRAINT fk_copilot_usage_business FOREIGN KEY (business_id) REFERENCES businesses (id),
    CONSTRAINT fk_copilot_usage_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_copilot_usage_conversation FOREIGN KEY (conversation_id) REFERENCES copilot_conversations (id),
    CONSTRAINT fk_copilot_usage_message FOREIGN KEY (message_id) REFERENCES copilot_messages (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO permissions (code, name, module) VALUES
    ('copilot:use', 'Use AI business assistant', 'copilot');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code = 'copilot:use';
