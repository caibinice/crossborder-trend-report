UPDATE market_configs
SET market_name = CASE market_key
      WHEN 'us' THEN '美国市场'
      WHEN 'sea' THEN '东南亚市场'
      ELSE market_name
    END,
    region = CASE market_key
      WHEN 'us' THEN '美国'
      WHEN 'sea' THEN '东南亚'
      ELSE region
    END,
    note = CASE market_key
      WHEN 'us' THEN '已接入 Google Trends 与美国公开商品目录'
      WHEN 'sea' THEN '当前以新加坡趋势与公开商品目录代表东南亚市场'
      ELSE note
    END
WHERE market_key IN ('us', 'sea');

UPDATE category_configs
SET category_name = CASE category_name
      WHEN 'Toys' THEN '玩具'
      WHEN 'Home & Living' THEN '家居'
      WHEN 'Beauty' THEN '美妆'
      WHEN 'Pet Supplies' THEN '宠物'
      WHEN 'Electronics' THEN '数码'
      WHEN 'Outdoors' THEN '户外'
      WHEN 'Baby' THEN '母婴'
      WHEN 'Kitchen' THEN '厨房'
      WHEN 'Fashion' THEN '服饰'
      WHEN 'Food' THEN '食品'
      WHEN 'Automotive' THEN '汽车'
      WHEN 'Stationery' THEN '文具'
      WHEN 'Health' THEN '健康'
      WHEN 'Beading' THEN '串珠'
      ELSE category_name
    END,
    keywords = CASE keywords
      WHEN 'Toys' THEN '玩具'
      WHEN 'Home & Living' THEN '家居'
      WHEN 'Beauty' THEN '美妆'
      WHEN 'Pet Supplies' THEN '宠物'
      WHEN 'Electronics' THEN '数码'
      WHEN 'Outdoors' THEN '户外'
      WHEN 'Baby' THEN '母婴'
      WHEN 'Kitchen' THEN '厨房'
      WHEN 'Fashion' THEN '服饰'
      WHEN 'Food' THEN '食品'
      WHEN 'Automotive' THEN '汽车'
      WHEN 'Stationery' THEN '文具'
      WHEN 'Health' THEN '健康'
      WHEN 'Beading' THEN '串珠'
      ELSE keywords
    END,
    note = '默认市场品类'
WHERE market_key IN ('us', 'sea');

UPDATE admin_settings
SET regions = '日本,美国,东南亚';

UPDATE sys_dict_data
SET dict_label = CASE dict_value
      WHEN 'us' THEN '美国'
      WHEN 'sea' THEN '东南亚'
      ELSE dict_label
    END
WHERE dict_type = 'market_region'
  AND dict_value IN ('us', 'sea');

UPDATE trend_reports
SET title = CASE
      WHEN source_key LIKE 'us:%' THEN CONCAT('美国市场跨境热品日报 ', DATE_FORMAT(report_date, '%Y-%m-%d'))
      WHEN source_key LIKE 'sea:%' THEN CONCAT('东南亚市场跨境热品日报 ', DATE_FORMAT(report_date, '%Y-%m-%d'))
      ELSE title
    END,
    summary = CASE
      WHEN source_key LIKE 'us:%' OR source_key LIKE 'sea:%'
        THEN CONCAT('历史采集日报已切换为中文展示；来源=', source_mode, '。重新采集后将生成完整中文商品翻译、采购词和摘要。')
      ELSE summary
    END
WHERE source_key LIKE 'us:%'
   OR source_key LIKE 'sea:%';

UPDATE trend_products products
JOIN trend_reports reports ON reports.id = products.report_id
SET products.category = CASE products.category
      WHEN 'Toys' THEN '玩具'
      WHEN 'Home & Living' THEN '家居'
      WHEN 'Beauty' THEN '美妆'
      WHEN 'Pet Supplies' THEN '宠物'
      WHEN 'Electronics' THEN '数码'
      WHEN 'Outdoors' THEN '户外'
      WHEN 'Baby' THEN '母婴'
      WHEN 'Kitchen' THEN '厨房'
      WHEN 'Fashion' THEN '服饰'
      WHEN 'Food' THEN '食品'
      WHEN 'Automotive' THEN '汽车'
      WHEN 'Stationery' THEN '文具'
      WHEN 'Health' THEN '健康'
      WHEN 'Beading' THEN '串珠'
      ELSE products.category
    END
WHERE reports.source_key LIKE 'us:%'
   OR reports.source_key LIKE 'sea:%';

UPDATE domestic_links links
JOIN trend_products products ON products.id = links.product_id
JOIN trend_reports reports ON reports.id = products.report_id
SET links.platform = CASE links.platform
      WHEN 'Taobao' THEN '淘宝'
      WHEN 'Pinduoduo' THEN '拼多多'
      ELSE links.platform
    END
WHERE reports.source_key LIKE 'us:%'
   OR reports.source_key LIKE 'sea:%';
