-- V36: Allow PDF rows in report_exports (PDF export was added after V20).

ALTER TABLE report_exports
    DROP CONSTRAINT chk_report_exports_format;

ALTER TABLE report_exports
    ADD CONSTRAINT chk_report_exports_format CHECK (export_format IN ('CSV', 'PDF'));
