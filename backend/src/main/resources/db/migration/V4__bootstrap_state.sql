CREATE TABLE IF NOT EXISTS app_bootstrap_state (
  bootstrap_key VARCHAR(64) NOT NULL,
  completed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (bootstrap_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
