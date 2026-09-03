-- =============================================================================
-- V22: Multi-step approval workflows
-- Phase 21 — sequential approval steps per rule, tracked via instances.
-- =============================================================================

CREATE TABLE approval_rule_steps (
    id                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    approval_rule_id    BIGINT UNSIGNED NOT NULL,
    step_order          INT             NOT NULL,
    name                VARCHAR(100)    NOT NULL,
    required_permission VARCHAR(100)    NOT NULL,
    created_at          TIMESTAMP(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at          TIMESTAMP(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),
    UNIQUE KEY uk_approval_rule_steps_order (approval_rule_id, step_order),
    INDEX idx_approval_rule_steps_rule (approval_rule_id),
    CONSTRAINT fk_approval_rule_steps_rule FOREIGN KEY (approval_rule_id) REFERENCES approval_rules (id),
    CONSTRAINT chk_approval_rule_steps_order CHECK (step_order >= 1)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE approval_instances (
    id                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    business_id         BIGINT UNSIGNED NOT NULL,
    approval_rule_id    BIGINT UNSIGNED NOT NULL,
    entity_type         VARCHAR(32)     NOT NULL,
    entity_id           BIGINT UNSIGNED NOT NULL,
    status              VARCHAR(16)     NOT NULL DEFAULT 'PENDING',
    current_step_order  INT             NOT NULL DEFAULT 1,
    total_steps         INT             NOT NULL,
    submitted_by        BIGINT UNSIGNED NOT NULL,
    submitted_at        TIMESTAMP(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    completed_at        TIMESTAMP(6)    NULL,
    created_at          TIMESTAMP(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at          TIMESTAMP(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),
    UNIQUE KEY uk_approval_instances_entity (business_id, entity_type, entity_id),
    INDEX idx_approval_instances_business (business_id),
    INDEX idx_approval_instances_status (status),
    INDEX idx_approval_instances_rule (approval_rule_id),
    CONSTRAINT fk_approval_instances_business FOREIGN KEY (business_id) REFERENCES businesses (id),
    CONSTRAINT fk_approval_instances_rule FOREIGN KEY (approval_rule_id) REFERENCES approval_rules (id),
    CONSTRAINT fk_approval_instances_submitted_by FOREIGN KEY (submitted_by) REFERENCES users (id),
    CONSTRAINT chk_approval_instances_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED')),
    CONSTRAINT chk_approval_instances_entity_type CHECK (
        entity_type IN ('INVENTORY_ADJUSTMENT', 'STOCK_TRANSFER', 'IMPORT_ORDER', 'STOCKTAKE')
    ),
    CONSTRAINT chk_approval_instances_step CHECK (current_step_order >= 1 AND total_steps >= 1)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE approval_instance_actions (
    id                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    approval_instance_id BIGINT UNSIGNED NOT NULL,
    step_order          INT             NOT NULL,
    action              VARCHAR(16)     NOT NULL,
    acted_by            BIGINT UNSIGNED NOT NULL,
    notes               VARCHAR(500)    NULL,
    acted_at            TIMESTAMP(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),
    INDEX idx_approval_instance_actions_instance (approval_instance_id),
    CONSTRAINT fk_approval_instance_actions_instance FOREIGN KEY (approval_instance_id) REFERENCES approval_instances (id),
    CONSTRAINT fk_approval_instance_actions_user FOREIGN KEY (acted_by) REFERENCES users (id),
    CONSTRAINT chk_approval_instance_actions_action CHECK (action IN ('APPROVED', 'REJECTED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- One default step per existing rule (backward compatible single-step approval)
INSERT INTO approval_rule_steps (approval_rule_id, step_order, name, required_permission)
SELECT r.id, 1, CONCAT(r.name, ' — step 1'), r.required_permission
FROM approval_rules r;
