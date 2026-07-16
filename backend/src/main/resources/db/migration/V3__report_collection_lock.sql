CREATE TABLE IF NOT EXISTS report_collection_locks (
  tenant_id VARCHAR(64) NOT NULL DEFAULT 'default',
  report_date DATE NOT NULL,
  source_key VARCHAR(128) NOT NULL,
  locked_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (tenant_id, report_date, source_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
