ALTER TABLE trend_reports
  ADD COLUMN source_key VARCHAR(128) NULL AFTER source_mode;

UPDATE trend_reports
SET source_key = CONCAT('legacy:', COALESCE(NULLIF(source_mode, ''), 'default'))
WHERE source_key IS NULL OR source_key = '';

ALTER TABLE trend_reports
  DROP INDEX uk_report_date_mode,
  MODIFY COLUMN source_key VARCHAR(128) NOT NULL,
  ADD UNIQUE KEY uk_report_date_source_key (tenant_id, report_date, source_key);
