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
INSERT INTO admin_settings(id, foreign_sources, domestic_sources, categories, regions, frequency_cron, max_products, jpy_cny_rate, default_shipping_cny, smart_mode)
SELECT 1, 'TikTok/Apify,Amazon/Rainforest,Amazon/Keepa', '1688,Taobao,Pinduoduo', '玩具,家居,美妆,宠物,数码,户外,母婴,汽车,厨房,文具,服饰,健康', '日本', '0 30 8 * * *', 30, 0.048000, 18.00, TRUE
WHERE NOT EXISTS (SELECT 1 FROM admin_settings WHERE id = 1);
