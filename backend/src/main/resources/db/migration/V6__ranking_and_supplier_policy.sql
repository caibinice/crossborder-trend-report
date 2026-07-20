ALTER TABLE admin_settings
  ADD COLUMN supplier_sites TEXT NULL AFTER domestic_sources,
  ADD COLUMN max_categories INT NOT NULL DEFAULT 10 AFTER max_products,
  ADD COLUMN products_per_category INT NOT NULL DEFAULT 10 AFTER max_categories,
  ADD COLUMN ranking_metric VARCHAR(32) NOT NULL DEFAULT 'sales_volume' AFTER products_per_category;

UPDATE admin_settings
SET max_products = 100,
    max_categories = 10,
    products_per_category = 10,
    ranking_metric = 'sales_volume',
    categories = '玩具,家居,美妆,宠物,数码,户外,母婴,厨房,服饰,食品',
    foreign_sources = CASE
      WHEN FIND_IN_SET('Rakuten', foreign_sources) > 0 THEN foreign_sources
      WHEN foreign_sources = '' THEN 'Rakuten,WooCommerce公开目录'
      ELSE CONCAT(foreign_sources, ',Rakuten')
    END,
    supplier_sites = CONCAT(
      '1688|https://s.1688.com/selloffer/offer_search.htm?keywords={keyword}', CHAR(10),
      '淘宝|https://s.taobao.com/search?q={keyword}', CHAR(10),
      '拼多多|https://mobile.yangkeduo.com/search_result.html?search_key={keyword}'
    )
WHERE tenant_id = 'default';

ALTER TABLE trend_products
  ADD COLUMN sales_volume_score DOUBLE NOT NULL DEFAULT 1 AFTER heat_score,
  ADD COLUMN sales_amount_score DOUBLE NOT NULL DEFAULT 1 AFTER sales_volume_score,
  ADD COLUMN ai_score DOUBLE NOT NULL DEFAULT 50 AFTER sales_amount_score;
