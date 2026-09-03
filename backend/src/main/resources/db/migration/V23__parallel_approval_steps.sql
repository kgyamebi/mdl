-- =============================================================================
-- V23: Parallel approval steps (Phase 26)
-- Multiple approver options at the same step_order (any-of).
-- =============================================================================

ALTER TABLE approval_rule_steps
    DROP INDEX uk_approval_rule_steps_order;

ALTER TABLE approval_rule_steps
    ADD UNIQUE KEY uk_approval_rule_steps_order_perm (approval_rule_id, step_order, required_permission);
