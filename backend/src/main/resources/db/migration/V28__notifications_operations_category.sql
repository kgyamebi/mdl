-- =============================================================================
-- V28: Allow OPERATIONS notification category (inventory/sales movement alerts)
-- Java publishes OPERATIONS events; V18 only allowed ALERT, SECURITY, APPROVAL, SYSTEM.
-- =============================================================================

ALTER TABLE notifications DROP CONSTRAINT chk_notifications_category;

ALTER TABLE notifications ADD CONSTRAINT chk_notifications_category CHECK (category IN (
    'ALERT', 'SECURITY', 'APPROVAL', 'SYSTEM', 'OPERATIONS'
));
