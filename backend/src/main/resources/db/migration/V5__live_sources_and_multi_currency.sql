ALTER TABLE admin_settings
  ADD COLUMN source_mode VARCHAR(16) NOT NULL DEFAULT 'external' AFTER regions,
  ADD COLUMN auto_exchange_rate BOOLEAN NOT NULL DEFAULT TRUE AFTER jpy_cny_rate;

UPDATE admin_settings
SET source_mode = 'external',
    foreign_sources = CASE
      WHEN FIND_IN_SET('WooCommerce公开目录', foreign_sources) > 0 THEN foreign_sources
      WHEN foreign_sources = '' THEN 'WooCommerce公开目录,Google Trends'
      ELSE CONCAT(foreign_sources, ',WooCommerce公开目录,Google Trends')
    END,
    categories = CASE
      WHEN FIND_IN_SET('食品', categories) > 0 THEN categories
      WHEN categories = '' THEN '食品'
      ELSE CONCAT(categories, ',食品')
    END;

ALTER TABLE trend_products
  ADD COLUMN source_price DECIMAL(14,4) NULL AFTER heat_score,
  ADD COLUMN source_currency VARCHAR(3) NOT NULL DEFAULT 'JPY' AFTER source_price,
  ADD COLUMN source_price_cny DECIMAL(14,2) NULL AFTER source_currency,
  ADD COLUMN image_url VARCHAR(1024) NULL AFTER source_url;

UPDATE trend_products
SET source_price = COALESCE(source_price, jp_price_jpy),
    source_currency = COALESCE(NULLIF(source_currency, ''), 'JPY'),
    source_price_cny = COALESCE(source_price_cny, jp_price_cny);

CREATE TABLE IF NOT EXISTS trend_signals (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  source_key VARCHAR(64) NOT NULL,
  region VARCHAR(16) NOT NULL,
  keyword VARCHAR(255) NOT NULL,
  traffic_label VARCHAR(32),
  traffic_value BIGINT NOT NULL DEFAULT 0,
  source_url VARCHAR(1024),
  image_url VARCHAR(1024),
  published_at TIMESTAMP NOT NULL,
  fetched_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_trend_signal (source_key, region, keyword, published_at),
  INDEX idx_trend_signal_region_time (region, published_at),
  INDEX idx_trend_signal_traffic (traffic_value)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS exchange_rates (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  base_currency VARCHAR(3) NOT NULL,
  quote_currency VARCHAR(3) NOT NULL,
  rate_date DATE NOT NULL,
  rate_value DECIMAL(18,8) NOT NULL,
  provider VARCHAR(64) NOT NULL,
  fetched_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_exchange_rate (base_currency, quote_currency, rate_date),
  INDEX idx_exchange_rate_pair_time (base_currency, quote_currency, rate_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS data_collection_runs (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  source_key VARCHAR(64) NOT NULL,
  trigger_type VARCHAR(32) NOT NULL,
  status VARCHAR(16) NOT NULL,
  item_count INT NOT NULL DEFAULT 0,
  message VARCHAR(1024),
  started_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  finished_at TIMESTAMP NULL,
  INDEX idx_collection_run_source_time (source_key, started_at),
  INDEX idx_collection_run_status_time (status, started_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
