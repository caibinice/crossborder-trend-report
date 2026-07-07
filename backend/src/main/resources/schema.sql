CREATE TABLE IF NOT EXISTS trend_reports (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  report_date DATE NOT NULL,
  source_mode VARCHAR(255) NOT NULL,
  title VARCHAR(255) NOT NULL,
  summary TEXT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_report_date_mode (report_date, source_mode)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS trend_products (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  report_id BIGINT NOT NULL,
  product_rank INT NOT NULL,
  category VARCHAR(64) NOT NULL,
  product_name_jp VARCHAR(255) NOT NULL,
  product_name_cn VARCHAR(255) NOT NULL,
  keywords VARCHAR(255) NOT NULL,
  source_platform VARCHAR(64) NOT NULL,
  source_url VARCHAR(1024),
  heat_score DOUBLE NOT NULL,
  jp_price_jpy DECIMAL(12,2), jp_price_cny DECIMAL(12,2), domestic_cost_cny DECIMAL(12,2), shipping_cny DECIMAL(12,2),
  estimated_profit_cny DECIMAL(12,2), estimated_margin DOUBLE, reason TEXT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_trend_products_report FOREIGN KEY (report_id) REFERENCES trend_reports(id) ON DELETE CASCADE,
  INDEX idx_trend_products_category (category), INDEX idx_trend_products_heat (heat_score)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS domestic_links (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  product_id BIGINT NOT NULL,
  platform VARCHAR(64) NOT NULL,
  title VARCHAR(255) NOT NULL,
  url VARCHAR(1024) NOT NULL,
  price_cny DECIMAL(12,2), note VARCHAR(255), created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_domestic_links_product FOREIGN KEY (product_id) REFERENCES trend_products(id) ON DELETE CASCADE,
  INDEX idx_domestic_links_product (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS admin_settings (
  id BIGINT PRIMARY KEY,
  foreign_sources VARCHAR(512) NOT NULL,
  domestic_sources VARCHAR(512) NOT NULL,
  categories VARCHAR(1024) NOT NULL,
  regions VARCHAR(512) NOT NULL,
  frequency_cron VARCHAR(64) NOT NULL,
  max_products INT NOT NULL,
  jpy_cny_rate DECIMAL(10,6) NOT NULL,
  default_shipping_cny DECIMAL(12,2) NOT NULL,
  smart_mode BOOLEAN NOT NULL,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS admin_roles (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  role_key VARCHAR(64) NOT NULL UNIQUE,
  role_name VARCHAR(64) NOT NULL,
  status VARCHAR(16) NOT NULL,
  remark VARCHAR(255),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS admin_users (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(64) NOT NULL UNIQUE,
  nickname VARCHAR(64) NOT NULL,
  role_key VARCHAR(64) NOT NULL,
  status VARCHAR(16) NOT NULL,
  email VARCHAR(128),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS admin_menus (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  parent_id BIGINT NOT NULL DEFAULT 0,
  menu_key VARCHAR(64) NOT NULL UNIQUE,
  title VARCHAR(64) NOT NULL,
  icon VARCHAR(64),
  path VARCHAR(128),
  component VARCHAR(128),
  sort_order INT NOT NULL DEFAULT 0,
  status VARCHAR(16) NOT NULL DEFAULT 'enabled'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS market_configs (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  market_key VARCHAR(64) NOT NULL UNIQUE,
  market_name VARCHAR(64) NOT NULL,
  region VARCHAR(64) NOT NULL,
  enabled BOOLEAN NOT NULL,
  note VARCHAR(255)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS category_configs (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  category_name VARCHAR(64) NOT NULL UNIQUE,
  market_key VARCHAR(64) NOT NULL,
  enabled BOOLEAN NOT NULL,
  keywords VARCHAR(255),
  note VARCHAR(255)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
