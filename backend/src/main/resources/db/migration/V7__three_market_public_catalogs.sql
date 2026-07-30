ALTER TABLE category_configs
  DROP INDEX uk_category_tenant_name,
  ADD UNIQUE KEY uk_category_tenant_market_name (tenant_id, market_key, category_name);

UPDATE trend_reports
SET source_key = CONCAT('jp:', source_key)
WHERE source_key NOT LIKE 'jp:%'
  AND source_key NOT LIKE 'us:%'
  AND source_key NOT LIKE 'sea:%';

UPDATE report_collection_locks
SET source_key = CONCAT('jp:', source_key)
WHERE source_key NOT LIKE 'jp:%'
  AND source_key NOT LIKE 'us:%'
  AND source_key NOT LIKE 'sea:%';

UPDATE market_configs
SET market_name = CASE market_key
      WHEN 'us' THEN 'United States'
      WHEN 'sea' THEN 'Southeast Asia'
      ELSE market_name
    END,
    region = CASE market_key
      WHEN 'us' THEN 'United States'
      WHEN 'sea' THEN 'Southeast Asia'
      ELSE region
    END,
    enabled = CASE WHEN market_key IN ('us','sea') THEN TRUE ELSE enabled END,
    note = CASE market_key
      WHEN 'us' THEN 'Google Trends and US public product catalogs are connected'
      WHEN 'sea' THEN 'Singapore represents the initial SEA trend and catalog feed'
      ELSE note
    END
WHERE market_key IN ('us','sea');

INSERT IGNORE INTO market_configs(tenant_id, market_key, market_name, region, enabled, note)
SELECT 'default', markets.market_key, markets.market_name, markets.region, TRUE, markets.note
FROM (
  SELECT 'us' AS market_key, 'United States' AS market_name, 'United States' AS region,
    'Google Trends and US public product catalogs are connected' AS note
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
  SELECT 'us' AS market_key
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
    regions = '日本,United States,Southeast Asia';

UPDATE sys_dict_data
SET dict_label = CASE dict_value
      WHEN 'us' THEN 'United States'
      WHEN 'sea' THEN 'Southeast Asia'
      ELSE dict_label
    END
WHERE dict_type = 'market_region'
  AND dict_value IN ('us','sea');
