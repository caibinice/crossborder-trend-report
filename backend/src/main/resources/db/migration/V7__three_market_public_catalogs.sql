ALTER TABLE category_configs
  DROP INDEX uk_category_tenant_name,
  ADD UNIQUE KEY uk_category_tenant_market_name (tenant_id, market_key, category_name);

DELETE legacy
FROM category_configs legacy
JOIN category_configs english
  ON english.tenant_id = legacy.tenant_id
  AND english.market_key = legacy.market_key
  AND english.category_name = CASE legacy.category_name
    WHEN '玩具' THEN 'Toys'
    WHEN '家居' THEN 'Home & Living'
    WHEN '美妆' THEN 'Beauty'
    WHEN '宠物' THEN 'Pet Supplies'
    WHEN '数码' THEN 'Electronics'
    WHEN '户外' THEN 'Outdoors'
    WHEN '母婴' THEN 'Baby'
    WHEN '厨房' THEN 'Kitchen'
    WHEN '服饰' THEN 'Fashion'
    WHEN '食品' THEN 'Food'
    WHEN '汽车' THEN 'Automotive'
    WHEN '文具' THEN 'Stationery'
    WHEN '健康' THEN 'Health'
  END
WHERE legacy.category_name IN ('玩具','家居','美妆','宠物','数码','户外','母婴','厨房','服饰','食品','汽车','文具','健康');

UPDATE category_configs
SET category_name = CASE category_name
  WHEN '玩具' THEN 'Toys'
  WHEN '家居' THEN 'Home & Living'
  WHEN '美妆' THEN 'Beauty'
  WHEN '宠物' THEN 'Pet Supplies'
  WHEN '数码' THEN 'Electronics'
  WHEN '户外' THEN 'Outdoors'
  WHEN '母婴' THEN 'Baby'
  WHEN '厨房' THEN 'Kitchen'
  WHEN '服饰' THEN 'Fashion'
  WHEN '食品' THEN 'Food'
  WHEN '汽车' THEN 'Automotive'
  WHEN '文具' THEN 'Stationery'
  WHEN '健康' THEN 'Health'
  ELSE category_name
END,
keywords = CASE keywords
  WHEN '玩具' THEN 'Toys'
  WHEN '家居' THEN 'Home & Living'
  WHEN '美妆' THEN 'Beauty'
  WHEN '宠物' THEN 'Pet Supplies'
  WHEN '数码' THEN 'Electronics'
  WHEN '户外' THEN 'Outdoors'
  WHEN '母婴' THEN 'Baby'
  WHEN '厨房' THEN 'Kitchen'
  WHEN '服饰' THEN 'Fashion'
  WHEN '食品' THEN 'Food'
  WHEN '汽车' THEN 'Automotive'
  WHEN '文具' THEN 'Stationery'
  WHEN '健康' THEN 'Health'
  ELSE keywords
END
WHERE category_name IN ('玩具','家居','美妆','宠物','数码','户外','母婴','厨房','服饰','食品','汽车','文具','健康');

UPDATE market_configs
SET market_name = CASE market_key
      WHEN 'jp' THEN 'Japan'
      WHEN 'us' THEN 'United States'
      WHEN 'sea' THEN 'Southeast Asia'
      ELSE market_name
    END,
    region = CASE market_key
      WHEN 'jp' THEN 'Japan'
      WHEN 'us' THEN 'United States'
      WHEN 'sea' THEN 'Southeast Asia'
      ELSE region
    END,
    enabled = CASE WHEN market_key IN ('jp','us','sea') THEN TRUE ELSE enabled END,
    note = CASE market_key
      WHEN 'jp' THEN 'Google Trends and public product catalogs are connected'
      WHEN 'us' THEN 'Google Trends and US public product catalogs are connected'
      WHEN 'sea' THEN 'Singapore represents the initial SEA trend and catalog feed'
      ELSE note
    END
WHERE market_key IN ('jp','us','sea');

INSERT IGNORE INTO market_configs(tenant_id, market_key, market_name, region, enabled, note)
SELECT 'default', markets.market_key, markets.market_name, markets.region, TRUE, markets.note
FROM (
  SELECT 'jp' AS market_key, 'Japan' AS market_name, 'Japan' AS region,
    'Google Trends and public product catalogs are connected' AS note
  UNION ALL SELECT 'us', 'United States', 'United States',
    'Google Trends and US public product catalogs are connected'
  UNION ALL SELECT 'sea', 'Southeast Asia', 'Southeast Asia',
    'Singapore represents the initial SEA trend and catalog feed'
) markets
JOIN (
  SELECT 1 AS ready
  FROM app_bootstrap_state
  WHERE bootstrap_key = 'default-seed-v1'
) bootstrap ON bootstrap.ready = 1;

INSERT IGNORE INTO category_configs(tenant_id, category_name, market_key, enabled, keywords, note)
SELECT 'default', categories.category_name, markets.market_key, TRUE, categories.category_name, 'Default market category'
FROM (
  SELECT 'Toys' AS category_name
  UNION ALL SELECT 'Home & Living'
  UNION ALL SELECT 'Beauty'
  UNION ALL SELECT 'Pet Supplies'
  UNION ALL SELECT 'Electronics'
  UNION ALL SELECT 'Outdoors'
  UNION ALL SELECT 'Baby'
  UNION ALL SELECT 'Kitchen'
  UNION ALL SELECT 'Fashion'
  UNION ALL SELECT 'Food'
) categories
CROSS JOIN (
  SELECT 'jp' AS market_key
  UNION ALL SELECT 'us'
  UNION ALL SELECT 'sea'
) markets
JOIN (
  SELECT 1 AS ready
  FROM app_bootstrap_state
  WHERE bootstrap_key = 'default-seed-v1'
) bootstrap ON bootstrap.ready = 1;

UPDATE admin_settings
SET products_per_category = 20,
    max_products = GREATEST(max_products, max_categories * 20),
    regions = 'Japan,United States,Southeast Asia',
    categories = REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(
      REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(
        categories,
        '玩具', 'Toys'),
        '家居', 'Home & Living'),
        '美妆', 'Beauty'),
        '宠物', 'Pet Supplies'),
        '数码', 'Electronics'),
        '户外', 'Outdoors'),
        '母婴', 'Baby'),
        '厨房', 'Kitchen'),
        '服饰', 'Fashion'),
        '食品', 'Food'),
        '汽车', 'Automotive'),
        '文具', 'Stationery'),
        '健康', 'Health'),
    foreign_sources = REPLACE(foreign_sources, 'WooCommerce公开目录', 'WooCommerce Public Catalog');

UPDATE trend_products
SET category = CASE category
  WHEN '玩具' THEN 'Toys'
  WHEN '家居' THEN 'Home & Living'
  WHEN '美妆' THEN 'Beauty'
  WHEN '宠物' THEN 'Pet Supplies'
  WHEN '数码' THEN 'Electronics'
  WHEN '户外' THEN 'Outdoors'
  WHEN '母婴' THEN 'Baby'
  WHEN '厨房' THEN 'Kitchen'
  WHEN '服饰' THEN 'Fashion'
  WHEN '食品' THEN 'Food'
  WHEN '汽车' THEN 'Automotive'
  WHEN '文具' THEN 'Stationery'
  WHEN '健康' THEN 'Health'
  ELSE category
END
WHERE category IN ('玩具','家居','美妆','宠物','数码','户外','母婴','厨房','服饰','食品','汽车','文具','健康');

UPDATE sys_config
SET config_name='Default language', config_value='en'
WHERE config_key='sys.lang';
