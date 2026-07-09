SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS trend_reports (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id VARCHAR(64) NOT NULL DEFAULT 'default',
  report_date DATE NOT NULL,
  source_mode VARCHAR(255) NOT NULL,
  title VARCHAR(255) NOT NULL,
  summary TEXT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_report_date_mode (tenant_id, report_date, source_mode)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS trend_products (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id VARCHAR(64) NOT NULL DEFAULT 'default',
  report_id BIGINT NOT NULL,
  product_rank INT NOT NULL,
  category VARCHAR(64) NOT NULL,
  product_name_jp VARCHAR(255) NOT NULL,
  product_name_cn VARCHAR(255) NOT NULL,
  keywords VARCHAR(255) NOT NULL,
  source_platform VARCHAR(64) NOT NULL,
  source_url VARCHAR(1024),
  heat_score DOUBLE NOT NULL,
  jp_price_jpy DECIMAL(12,2),
  jp_price_cny DECIMAL(12,2),
  domestic_cost_cny DECIMAL(12,2),
  shipping_cny DECIMAL(12,2),
  estimated_profit_cny DECIMAL(12,2),
  estimated_margin DOUBLE,
  reason TEXT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_trend_products_report FOREIGN KEY (report_id) REFERENCES trend_reports(id) ON DELETE CASCADE,
  INDEX idx_trend_products_tenant_category (tenant_id, category),
  INDEX idx_trend_products_heat (heat_score)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS domestic_links (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id VARCHAR(64) NOT NULL DEFAULT 'default',
  product_id BIGINT NOT NULL,
  platform VARCHAR(64) NOT NULL,
  title VARCHAR(255) NOT NULL,
  url VARCHAR(1024) NOT NULL,
  price_cny DECIMAL(12,2),
  note VARCHAR(255),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_domestic_links_product FOREIGN KEY (product_id) REFERENCES trend_products(id) ON DELETE CASCADE,
  INDEX idx_domestic_links_tenant_product (tenant_id, product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS admin_settings (
  id BIGINT PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL DEFAULT 'default',
  foreign_sources VARCHAR(512) NOT NULL,
  domestic_sources VARCHAR(512) NOT NULL,
  categories VARCHAR(1024) NOT NULL,
  regions VARCHAR(512) NOT NULL,
  frequency_cron VARCHAR(64) NOT NULL,
  max_products INT NOT NULL,
  jpy_cny_rate DECIMAL(10,6) NOT NULL,
  default_shipping_cny DECIMAL(12,2) NOT NULL,
  smart_mode BOOLEAN NOT NULL,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_admin_settings_tenant (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS sys_tenant (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id VARCHAR(64) NOT NULL UNIQUE,
  tenant_name VARCHAR(128) NOT NULL,
  contact_user VARCHAR(64),
  contact_phone VARCHAR(64),
  package_name VARCHAR(128),
  status VARCHAR(16) NOT NULL DEFAULT 'enabled',
  remark VARCHAR(255),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS sys_role (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id VARCHAR(64) NOT NULL DEFAULT 'default',
  role_key VARCHAR(64) NOT NULL,
  role_name VARCHAR(64) NOT NULL,
  status VARCHAR(16) NOT NULL DEFAULT 'enabled',
  menu_keys TEXT,
  remark VARCHAR(255),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_role_tenant_key (tenant_id, role_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS sys_user (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id VARCHAR(64) NOT NULL DEFAULT 'default',
  username VARCHAR(64) NOT NULL UNIQUE,
  password VARCHAR(128) NOT NULL DEFAULT 'admin',
  nickname VARCHAR(64) NOT NULL,
  role_key VARCHAR(64) NOT NULL,
  status VARCHAR(16) NOT NULL DEFAULT 'enabled',
  email VARCHAR(128),
  phone VARCHAR(64),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS sys_menu (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  parent_id BIGINT NOT NULL DEFAULT 0,
  menu_key VARCHAR(64) NOT NULL UNIQUE,
  title VARCHAR(64) NOT NULL,
  icon VARCHAR(64),
  path VARCHAR(128),
  component VARCHAR(128),
  permission VARCHAR(128),
  sort_order INT NOT NULL DEFAULT 0,
  status VARCHAR(16) NOT NULL DEFAULT 'enabled'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS sys_dict_type (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id VARCHAR(64) NOT NULL DEFAULT 'default',
  dict_name VARCHAR(128) NOT NULL,
  dict_type VARCHAR(128) NOT NULL,
  status VARCHAR(16) NOT NULL DEFAULT 'enabled',
  remark VARCHAR(255),
  UNIQUE KEY uk_dict_type_tenant (tenant_id, dict_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS sys_dict_data (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id VARCHAR(64) NOT NULL DEFAULT 'default',
  dict_type VARCHAR(128) NOT NULL,
  dict_label VARCHAR(128) NOT NULL,
  dict_value VARCHAR(128) NOT NULL,
  sort_order INT NOT NULL DEFAULT 0,
  status VARCHAR(16) NOT NULL DEFAULT 'enabled',
  remark VARCHAR(255)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS sys_config (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id VARCHAR(64) NOT NULL DEFAULT 'default',
  config_name VARCHAR(128) NOT NULL,
  config_key VARCHAR(128) NOT NULL,
  config_value VARCHAR(512),
  system_builtin BOOLEAN NOT NULL DEFAULT FALSE,
  remark VARCHAR(255),
  UNIQUE KEY uk_config_tenant_key (tenant_id, config_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS sys_oper_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id VARCHAR(64) NOT NULL DEFAULT 'default',
  username VARCHAR(64),
  module VARCHAR(64),
  action VARCHAR(64),
  method VARCHAR(255),
  status VARCHAR(16),
  message VARCHAR(1024),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS sys_login_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id VARCHAR(64) NOT NULL DEFAULT 'default',
  username VARCHAR(64),
  ipaddr VARCHAR(64),
  status VARCHAR(16),
  message VARCHAR(512),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS market_configs (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id VARCHAR(64) NOT NULL DEFAULT 'default',
  market_key VARCHAR(64) NOT NULL,
  market_name VARCHAR(64) NOT NULL,
  region VARCHAR(64) NOT NULL,
  enabled BOOLEAN NOT NULL,
  note VARCHAR(255),
  UNIQUE KEY uk_market_tenant_key (tenant_id, market_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS category_configs (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id VARCHAR(64) NOT NULL DEFAULT 'default',
  category_name VARCHAR(64) NOT NULL,
  market_key VARCHAR(64) NOT NULL,
  enabled BOOLEAN NOT NULL,
  keywords VARCHAR(255),
  note VARCHAR(255),
  UNIQUE KEY uk_category_tenant_name (tenant_id, category_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO admin_settings (
  id, tenant_id, foreign_sources, domestic_sources, categories, regions,
  frequency_cron, max_products, jpy_cny_rate, default_shipping_cny, smart_mode
) VALUES (
  1, 'default',
  'TikTok/Apify,Amazon/Rainforest,Amazon/Keepa',
  '1688,Taobao,Pinduoduo',
  '玩具,家居,美妆,宠物,数码,户外,母婴,汽车,厨房,文具,服饰,健康',
  '日本,美国,东南亚',
  '0 30 8 * * *',
  30,
  0.048000,
  18.00,
  TRUE
)
ON DUPLICATE KEY UPDATE
  foreign_sources = VALUES(foreign_sources),
  domestic_sources = VALUES(domestic_sources),
  categories = VALUES(categories),
  regions = VALUES(regions),
  frequency_cron = VALUES(frequency_cron),
  max_products = VALUES(max_products),
  jpy_cny_rate = VALUES(jpy_cny_rate),
  default_shipping_cny = VALUES(default_shipping_cny),
  smart_mode = VALUES(smart_mode);

INSERT INTO sys_tenant (
  id, tenant_id, tenant_name, contact_user, contact_phone, package_name, status, remark
) VALUES (
  1, 'default', '默认租户', '系统管理员', '', '基础套餐', 'enabled', '跨境趋势报表默认租户'
)
ON DUPLICATE KEY UPDATE
  tenant_name = VALUES(tenant_name),
  contact_user = VALUES(contact_user),
  contact_phone = VALUES(contact_phone),
  package_name = VALUES(package_name),
  status = VALUES(status),
  remark = VALUES(remark);

INSERT INTO sys_role (
  id, tenant_id, role_key, role_name, status, menu_keys, remark
) VALUES
  (1, 'default', 'admin', '超级管理员', 'enabled', '*', '系统内置超级管理员'),
  (2, 'default', 'tenant_admin', '租户管理员', 'enabled', 'dashboard,selection,reports', '租户侧配置与运营'),
  (3, 'default', 'operator', '运营人员', 'enabled', 'dashboard,reports', '选品与报表查看')
ON DUPLICATE KEY UPDATE
  role_name = VALUES(role_name),
  status = VALUES(status),
  menu_keys = VALUES(menu_keys),
  remark = VALUES(remark);

INSERT INTO sys_user (
  id, tenant_id, username, password, nickname, role_key, status, email, phone
) VALUES (
  1, 'default', 'admin', 'admin', '系统管理员', 'admin', 'enabled', 'admin@example.com', ''
)
ON DUPLICATE KEY UPDATE
  tenant_id = VALUES(tenant_id),
  password = VALUES(password),
  nickname = VALUES(nickname),
  role_key = VALUES(role_key),
  status = VALUES(status),
  email = VALUES(email),
  phone = VALUES(phone);

INSERT INTO sys_menu (
  id, parent_id, menu_key, title, icon, path, component, permission, sort_order, status
) VALUES
  (1, 0, 'dashboard', '首页 / 工作台', 'dashboard', '/admin', 'Dashboard', 'dashboard:view', 1, 'enabled'),
  (2, 0, 'system', '系统管理', 'system', '/admin/system', 'Layout', 'system:view', 10, 'enabled'),
  (3, 2, 'tenants', '租户管理', 'tenant', '/admin/system/tenants', 'TenantManage', 'system:tenant:list', 11, 'enabled'),
  (4, 2, 'users', '用户管理', 'user', '/admin/system/users', 'UserManage', 'system:user:list', 12, 'enabled'),
  (5, 2, 'roles', '角色管理', 'peoples', '/admin/system/roles', 'RoleManage', 'system:role:list', 13, 'enabled'),
  (6, 2, 'menus', '菜单管理', 'tree-table', '/admin/system/menus', 'MenuManage', 'system:menu:list', 14, 'enabled'),
  (7, 2, 'dict', '字典管理', 'dict', '/admin/system/dict', 'DictManage', 'system:dict:list', 15, 'enabled'),
  (8, 2, 'configs', '参数配置', 'config', '/admin/system/configs', 'ConfigManage', 'system:config:list', 16, 'enabled'),
  (9, 2, 'logs', '日志管理', 'log', '/admin/system/logs', 'LogManage', 'system:log:list', 17, 'enabled'),
  (10, 0, 'selection', '选品配置', 'shopping', '/admin/selection', 'Layout', 'selection:view', 20, 'enabled'),
  (11, 10, 'sources', '数据源配置', 'link', '/admin/selection/sources', 'SourceConfig', 'selection:source:list', 21, 'enabled'),
  (12, 10, 'markets', '市场配置', 'international', '/admin/selection/markets', 'MarketConfig', 'selection:market:list', 22, 'enabled'),
  (13, 10, 'categories', '品类配置', 'category', '/admin/selection/categories', 'CategoryConfig', 'selection:category:list', 23, 'enabled'),
  (14, 10, 'schedules', '采集频率配置', 'time', '/admin/selection/schedules', 'ScheduleConfig', 'selection:schedule:list', 24, 'enabled'),
  (15, 0, 'reports', '报表管理', 'chart', '/admin/reports', 'Layout', 'reports:view', 30, 'enabled'),
  (16, 15, 'dailyReports', '日报记录', 'date', '/admin/reports/daily', 'DailyReports', 'reports:daily:list', 31, 'enabled'),
  (17, 15, 'productPool', '商品池', 'goods', '/admin/reports/products', 'ProductPool', 'reports:product:list', 32, 'enabled')
ON DUPLICATE KEY UPDATE
  parent_id = VALUES(parent_id),
  title = VALUES(title),
  icon = VALUES(icon),
  path = VALUES(path),
  component = VALUES(component),
  permission = VALUES(permission),
  sort_order = VALUES(sort_order),
  status = VALUES(status);

INSERT INTO sys_dict_type (
  id, tenant_id, dict_name, dict_type, status, remark
) VALUES
  (1, 'default', '系统状态', 'sys_normal_disable', 'enabled', '启停通用状态'),
  (2, 'default', '市场区域', 'market_region', 'enabled', '跨境市场区域字典')
ON DUPLICATE KEY UPDATE
  dict_name = VALUES(dict_name),
  status = VALUES(status),
  remark = VALUES(remark);

INSERT INTO sys_dict_data (
  id, tenant_id, dict_type, dict_label, dict_value, sort_order, status, remark
) VALUES
  (1, 'default', 'sys_normal_disable', '正常', 'enabled', 1, 'enabled', ''),
  (2, 'default', 'sys_normal_disable', '停用', 'disabled', 2, 'enabled', ''),
  (3, 'default', 'market_region', '日本', 'jp', 1, 'enabled', ''),
  (4, 'default', 'market_region', '美国', 'us', 2, 'enabled', ''),
  (5, 'default', 'market_region', '东南亚', 'sea', 3, 'enabled', '')
ON DUPLICATE KEY UPDATE
  tenant_id = VALUES(tenant_id),
  dict_type = VALUES(dict_type),
  dict_label = VALUES(dict_label),
  dict_value = VALUES(dict_value),
  sort_order = VALUES(sort_order),
  status = VALUES(status),
  remark = VALUES(remark);

INSERT INTO sys_config (
  id, tenant_id, config_name, config_key, config_value, system_builtin, remark
) VALUES
  (1, 'default', '系统名称', 'sys.name', '跨境选品系统', TRUE, ''),
  (2, 'default', '默认语言', 'sys.lang', 'zh-CN', TRUE, '')
ON DUPLICATE KEY UPDATE
  config_name = VALUES(config_name),
  config_value = VALUES(config_value),
  system_builtin = VALUES(system_builtin),
  remark = VALUES(remark);

INSERT INTO market_configs (
  id, tenant_id, market_key, market_name, region, enabled, note
) VALUES
  (1, 'default', 'jp', '日本市场', '日本', TRUE, '已接入演示趋势数据，可继续接 TikTok/Amazon JP'),
  (2, 'default', 'us', '美国市场', '美国', FALSE, '待接入 Amazon US / TikTok US 数据源'),
  (3, 'default', 'sea', '东南亚市场', '东南亚', FALSE, '待接入 TikTok Shop SEA / Shopee / Lazada')
ON DUPLICATE KEY UPDATE
  market_name = VALUES(market_name),
  region = VALUES(region),
  enabled = VALUES(enabled),
  note = VALUES(note);

INSERT INTO category_configs (
  id, tenant_id, category_name, market_key, enabled, keywords, note
) VALUES
  (1, 'default', '玩具', 'jp', TRUE, '玩具,盲盒,摆件', '日本市场默认品类'),
  (2, 'default', '家居', 'jp', TRUE, '家居,收纳,厨房', '日本市场默认品类'),
  (3, 'default', '美妆', 'jp', TRUE, '美妆,修容,化妆刷', '日本市场默认品类'),
  (4, 'default', '宠物', 'jp', TRUE, '宠物,猫狗用品', '日本市场默认品类'),
  (5, 'default', '数码', 'jp', TRUE, '数码,配件,散热', '日本市场默认品类'),
  (6, 'default', '户外', 'jp', TRUE, '户外,露营,便携', '日本市场默认品类'),
  (7, 'default', '母婴', 'jp', TRUE, '母婴,儿童,喂养', '日本市场默认品类'),
  (8, 'default', '汽车', 'jp', TRUE, '汽车,收纳,车载', '日本市场默认品类'),
  (9, 'default', '厨房', 'jp', TRUE, '厨房,切菜,防油', '日本市场默认品类'),
  (10, 'default', '文具', 'jp', TRUE, '文具,学习,办公', '日本市场默认品类'),
  (11, 'default', '服饰', 'jp', TRUE, '服饰,夏季,防晒', '日本市场默认品类'),
  (12, 'default', '健康', 'jp', TRUE, '健康,坐姿,办公', '日本市场默认品类')
ON DUPLICATE KEY UPDATE
  market_key = VALUES(market_key),
  enabled = VALUES(enabled),
  keywords = VALUES(keywords),
  note = VALUES(note);

INSERT INTO trend_reports (
  id, tenant_id, report_date, source_mode, title, summary
) VALUES
(
  1,
  'default',
  '2026-07-08',
  'TikTok/Apify + Amazon/Rainforest + Amazon/Keepa',
  '日本跨境热品日报 2026-07-08',
  '工作日热度最高的一组样例，覆盖玩具、家居、数码、美妆与厨房品类。'
),
(
  2,
  'default',
  '2026-07-07',
  'TikTok/Apify + Amazon/Rainforest + Amazon/Keepa',
  '日本跨境热品日报 2026-07-07',
  '偏向宠物、户外和办公文具类，适合测试趋势切换和历史日报列表。'
),
(
  3,
  'default',
  '2026-07-06',
  'TikTok/Apify + Amazon/Rainforest + Amazon/Keepa',
  '日本跨境热品日报 2026-07-06',
  '覆盖服饰、母婴、健康类产品，用于验证多日报与多品类场景。'
)
ON DUPLICATE KEY UPDATE
  tenant_id = VALUES(tenant_id),
  report_date = VALUES(report_date),
  source_mode = VALUES(source_mode),
  title = VALUES(title),
  summary = VALUES(summary);

INSERT INTO trend_products (
  id, tenant_id, report_id, product_rank, category, product_name_jp, product_name_cn, keywords,
  source_platform, source_url, heat_score, jp_price_jpy, jp_price_cny, domestic_cost_cny,
  shipping_cny, estimated_profit_cny, estimated_margin, reason
) VALUES
  (
    1, 'default', 1, 1, '玩具',
    'ミニチュアブラインドボックス', '迷你盲盒摆件', '迷你 盲盒 摆件',
    'TikTok JP / demo', 'https://www.tiktok.com/search?q=%E8%BF%B7%E4%BD%A0%20%E7%9B%B2%E7%9B%92%20%E6%91%86%E4%BB%B6',
    96, 1980.00, 95.04, 29.90, 18.00, 47.14, 0.4959,
    '开箱内容传播性强，适合做低客单高转化的爆品测试。'
  ),
  (
    2, 'default', 1, 2, '家居',
    '折りたたみ水切りラック', '折叠沥水架', '折叠 沥水架 厨房',
    'TikTok JP / demo', 'https://www.tiktok.com/search?q=%E6%8A%98%E5%8F%A0%20%E6%B2%A5%E6%B0%B4%E6%9E%B6%20%E5%8E%A8%E6%88%BF',
    89, 2480.00, 119.04, 39.80, 18.00, 61.24, 0.5145,
    '日本厨房小空间场景明显，适合与收纳类内容联动。'
  ),
  (
    3, 'default', 1, 3, '数码',
    'スマホ冷却ファン', '手机散热背夹', '手机 散热 背夹',
    'TikTok JP / demo', 'https://www.tiktok.com/search?q=%E6%89%8B%E6%9C%BA%20%E6%95%A3%E7%83%AD%20%E8%83%8C%E5%A4%B9',
    84, 3280.00, 157.44, 66.00, 18.00, 73.44, 0.4664,
    '夏季直播/手游场景稳定，适合搭配视频创作者周边内容。'
  ),
  (
    4, 'default', 1, 4, '美妆',
    'LEDコンパクトメイクミラー', '便携补光化妆镜', '补光 化妆镜 便携',
    'TikTok JP / demo', 'https://www.tiktok.com/search?q=%E8%A1%A5%E5%85%89%20%E5%8C%96%E5%A6%86%E9%95%9C%20%E4%BE%BF%E6%90%BA',
    81, 2680.00, 128.64, 42.50, 16.00, 70.14, 0.5452,
    '适合与出差、通勤、随身美妆场景结合，视频展示空间大。'
  ),
  (
    5, 'default', 1, 5, '厨房',
    '真空保存ボックス', '真空密封保鲜盒', '真空 保鲜盒 密封',
    'TikTok JP / demo', 'https://www.tiktok.com/search?q=%E7%9C%9F%E7%A9%BA%20%E4%BF%9D%E9%B2%9C%E7%9B%92%20%E5%AF%86%E5%B0%81',
    78, 2180.00, 104.64, 33.60, 17.00, 54.04, 0.5164,
    '适合厨房收纳和冰箱整理内容，容易与保鲜痛点形成转化。'
  ),
  (
    6, 'default', 2, 1, '宠物',
    '自動給餌給水セット', '自动喂食饮水器', '宠物 自动 喂食 饮水器',
    'TikTok JP / demo', 'https://www.tiktok.com/search?q=%E5%AE%A0%E7%89%A9%20%E8%87%AA%E5%8A%A8%20%E5%96%82%E9%A3%9F',
    92, 4580.00, 219.84, 96.00, 24.00, 99.84, 0.4541,
    '猫狗双场景都容易出内容，适合做功能演示和居家养宠题材。'
  ),
  (
    7, 'default', 2, 2, '户外',
    '軽量折りたたみキャンプワゴン', '轻量折叠露营推车', '露营 推车 折叠 轻量',
    'TikTok JP / demo', 'https://www.tiktok.com/search?q=%E9%9C%B2%E8%90%A5%20%E6%8E%A8%E8%BD%A6%20%E6%8A%98%E5%8F%A0',
    87, 6980.00, 335.04, 168.00, 38.00, 129.04, 0.3851,
    '日本露营场景成熟，适合长视频对比和强收纳卖点表达。'
  ),
  (
    8, 'default', 2, 3, '文具',
    'タブレット磁気スタンド', '平板磁吸阅读支架', '平板 支架 磁吸 阅读',
    'TikTok JP / demo', 'https://www.tiktok.com/search?q=%E5%B9%B3%E6%9D%BF%20%E6%94%AF%E6%9E%B6%20%E7%A3%81%E5%90%B8',
    76, 2880.00, 138.24, 52.00, 18.00, 68.24, 0.4936,
    '办公和追剧两类场景通吃，适合与桌搭内容搭配推广。'
  ),
  (
    9, 'default', 3, 1, '服饰',
    'UVアームカバー', '防晒冰袖', '防晒 冰袖 夏季',
    'TikTok JP / demo', 'https://www.tiktok.com/search?q=%E9%98%B2%E6%99%92%20%E5%86%B0%E8%A2%96%20%E5%A4%8F%E5%AD%A3',
    88, 1580.00, 75.84, 16.80, 10.00, 49.04, 0.6466,
    '夏季通勤与骑行场景明显，低客单适合快速验证转化素材。'
  ),
  (
    10, 'default', 3, 2, '母婴',
    'ベビーフード小分けケース', '婴儿辅食分装盒', '母婴 辅食 分装盒',
    'TikTok JP / demo', 'https://www.tiktok.com/search?q=%E8%BE%85%E9%A3%9F%20%E5%88%86%E8%A3%85%E7%9B%92%20%E6%AF%8D%E5%A9%B4',
    82, 2380.00, 114.24, 28.50, 15.00, 70.74, 0.6192,
    '辅食准备流程容易形成种草内容，适合母婴博主短视频推荐。'
  ),
  (
    11, 'default', 3, 3, '健康',
    '姿勢サポートクッション', '坐姿矫正靠背', '坐姿 矫正 靠背 办公',
    'TikTok JP / demo', 'https://www.tiktok.com/search?q=%E5%9D%90%E5%A7%BF%20%E7%9F%AB%E6%AD%A3%20%E9%9D%A0%E8%83%8C',
    79, 3680.00, 176.64, 72.00, 22.00, 82.64, 0.4678,
    '久坐办公人群需求稳定，适合通过前后对比图和使用反馈转化。'
  )
ON DUPLICATE KEY UPDATE
  tenant_id = VALUES(tenant_id),
  report_id = VALUES(report_id),
  product_rank = VALUES(product_rank),
  category = VALUES(category),
  product_name_jp = VALUES(product_name_jp),
  product_name_cn = VALUES(product_name_cn),
  keywords = VALUES(keywords),
  source_platform = VALUES(source_platform),
  source_url = VALUES(source_url),
  heat_score = VALUES(heat_score),
  jp_price_jpy = VALUES(jp_price_jpy),
  jp_price_cny = VALUES(jp_price_cny),
  domestic_cost_cny = VALUES(domestic_cost_cny),
  shipping_cny = VALUES(shipping_cny),
  estimated_profit_cny = VALUES(estimated_profit_cny),
  estimated_margin = VALUES(estimated_margin),
  reason = VALUES(reason);

INSERT INTO domestic_links (
  id, tenant_id, product_id, platform, title, url, price_cny, note
) VALUES
  (
    1, 'default', 1, '1688', '迷你盲盒摆件 - 1688 搜索',
    'https://s.1688.com/selloffer/offer_search.htm?keywords=%E8%BF%B7%E4%BD%A0%20%E7%9B%B2%E7%9B%92%20%E6%91%86%E4%BB%B6',
    29.90, '默认搜索入口，后续可接 1688 开放平台精确拿价'
  ),
  (
    2, 'default', 1, 'Taobao', '迷你盲盒摆件 - 淘宝搜索',
    'https://s.taobao.com/search?q=%E8%BF%B7%E4%BD%A0%20%E7%9B%B2%E7%9B%92%20%E6%91%86%E4%BB%B6',
    36.90, '适合核对零售图文卖点'
  ),
  (
    3, 'default', 2, '1688', '折叠沥水架 - 1688 搜索',
    'https://s.1688.com/selloffer/offer_search.htm?keywords=%E6%8A%98%E5%8F%A0%20%E6%B2%A5%E6%B0%B4%E6%9E%B6%20%E5%8E%A8%E6%88%BF',
    39.80, '用于比对工厂拿货价'
  ),
  (
    4, 'default', 2, 'Pinduoduo', '折叠沥水架 - 拼多多搜索',
    'https://mobile.yangkeduo.com/search_result.html?search_key=%E6%8A%98%E5%8F%A0%20%E6%B2%A5%E6%B0%B4%E6%9E%B6%20%E5%8E%A8%E6%88%BF',
    45.50, '适合看低价带竞品'
  ),
  (
    5, 'default', 3, '1688', '手机散热背夹 - 1688 搜索',
    'https://s.1688.com/selloffer/offer_search.htm?keywords=%E6%89%8B%E6%9C%BA%20%E6%95%A3%E7%83%AD%20%E8%83%8C%E5%A4%B9',
    66.00, '适合看 OEM/ODM 供货'
  ),
  (
    6, 'default', 3, 'Taobao', '手机散热背夹 - 淘宝搜索',
    'https://s.taobao.com/search?q=%E6%89%8B%E6%9C%BA%20%E6%95%A3%E7%83%AD%20%E8%83%8C%E5%A4%B9',
    79.00, '适合看零售端卖点和主图'
  ),
  (
    7, 'default', 4, '1688', '便携补光化妆镜 - 1688 搜索',
    'https://s.1688.com/selloffer/offer_search.htm?keywords=%E8%A1%A5%E5%85%89%20%E5%8C%96%E5%A6%86%E9%95%9C%20%E4%BE%BF%E6%90%BA',
    42.50, '适合看补光镜源头款和起订量'
  ),
  (
    8, 'default', 4, 'Taobao', '便携补光化妆镜 - 淘宝搜索',
    'https://s.taobao.com/search?q=%E8%A1%A5%E5%85%89%20%E5%8C%96%E5%A6%86%E9%95%9C%20%E4%BE%BF%E6%90%BA',
    58.00, '适合核对包装图和功能点展示'
  ),
  (
    9, 'default', 5, '1688', '真空密封保鲜盒 - 1688 搜索',
    'https://s.1688.com/selloffer/offer_search.htm?keywords=%E7%9C%9F%E7%A9%BA%20%E4%BF%9D%E9%B2%9C%E7%9B%92%20%E5%AF%86%E5%B0%81',
    33.60, '适合看家庭收纳类大货供应'
  ),
  (
    10, 'default', 5, 'Pinduoduo', '真空密封保鲜盒 - 拼多多搜索',
    'https://mobile.yangkeduo.com/search_result.html?search_key=%E7%9C%9F%E7%A9%BA%20%E4%BF%9D%E9%B2%9C%E7%9B%92%20%E5%AF%86%E5%B0%81',
    39.90, '适合观察低价带商品组合形式'
  ),
  (
    11, 'default', 6, '1688', '自动喂食饮水器 - 1688 搜索',
    'https://s.1688.com/selloffer/offer_search.htm?keywords=%E5%AE%A0%E7%89%A9%20%E8%87%AA%E5%8A%A8%20%E5%96%82%E9%A3%9F',
    96.00, '适合看宠物器具结构和供货规格'
  ),
  (
    12, 'default', 6, 'Taobao', '自动喂食饮水器 - 淘宝搜索',
    'https://s.taobao.com/search?q=%E5%AE%A0%E7%89%A9%20%E8%87%AA%E5%8A%A8%20%E5%96%82%E9%A3%9F',
    128.00, '适合核对宠物用户评论和使用痛点'
  ),
  (
    13, 'default', 7, '1688', '轻量折叠露营推车 - 1688 搜索',
    'https://s.1688.com/selloffer/offer_search.htm?keywords=%E9%9C%B2%E8%90%A5%20%E6%8E%A8%E8%BD%A6%20%E6%8A%98%E5%8F%A0',
    168.00, '适合对比不同承重和轮组方案'
  ),
  (
    14, 'default', 7, 'Taobao', '轻量折叠露营推车 - 淘宝搜索',
    'https://s.taobao.com/search?q=%E9%9C%B2%E8%90%A5%20%E6%8E%A8%E8%BD%A6%20%E6%8A%98%E5%8F%A0',
    228.00, '适合看零售端卖点包装和主图风格'
  ),
  (
    15, 'default', 8, '1688', '平板磁吸阅读支架 - 1688 搜索',
    'https://s.1688.com/selloffer/offer_search.htm?keywords=%E5%B9%B3%E6%9D%BF%20%E6%94%AF%E6%9E%B6%20%E7%A3%81%E5%90%B8',
    52.00, '适合看办公桌搭类配件供货'
  ),
  (
    16, 'default', 8, 'Pinduoduo', '平板磁吸阅读支架 - 拼多多搜索',
    'https://mobile.yangkeduo.com/search_result.html?search_key=%E5%B9%B3%E6%9D%BF%20%E6%94%AF%E6%9E%B6%20%E7%A3%81%E5%90%B8',
    64.00, '适合看低价带支架与磁吸卖点组合'
  ),
  (
    17, 'default', 9, '1688', '防晒冰袖 - 1688 搜索',
    'https://s.1688.com/selloffer/offer_search.htm?keywords=%E9%98%B2%E6%99%92%20%E5%86%B0%E8%A2%96%20%E5%A4%8F%E5%AD%A3',
    16.80, '适合快速对比夏季爆款起订量'
  ),
  (
    18, 'default', 9, 'Taobao', '防晒冰袖 - 淘宝搜索',
    'https://s.taobao.com/search?q=%E9%98%B2%E6%99%92%20%E5%86%B0%E8%A2%96%20%E5%A4%8F%E5%AD%A3',
    26.90, '适合核对图案和尺码变化'
  ),
  (
    19, 'default', 10, '1688', '婴儿辅食分装盒 - 1688 搜索',
    'https://s.1688.com/selloffer/offer_search.htm?keywords=%E8%BE%85%E9%A3%9F%20%E5%88%86%E8%A3%85%E7%9B%92%20%E6%AF%8D%E5%A9%B4',
    28.50, '适合看食品接触级材质说明'
  ),
  (
    20, 'default', 10, 'Taobao', '婴儿辅食分装盒 - 淘宝搜索',
    'https://s.taobao.com/search?q=%E8%BE%85%E9%A3%9F%20%E5%88%86%E8%A3%85%E7%9B%92%20%E6%AF%8D%E5%A9%B4',
    39.90, '适合核对母婴场景图和收纳卖点'
  ),
  (
    21, 'default', 11, '1688', '坐姿矫正靠背 - 1688 搜索',
    'https://s.1688.com/selloffer/offer_search.htm?keywords=%E5%9D%90%E5%A7%BF%20%E7%9F%AB%E6%AD%A3%20%E9%9D%A0%E8%83%8C',
    72.00, '适合看人体工学类工厂款式'
  ),
  (
    22, 'default', 11, 'Pinduoduo', '坐姿矫正靠背 - 拼多多搜索',
    'https://mobile.yangkeduo.com/search_result.html?search_key=%E5%9D%90%E5%A7%BF%20%E7%9F%AB%E6%AD%A3%20%E9%9D%A0%E8%83%8C',
    89.00, '适合看低价带办公健康类竞品'
  )
ON DUPLICATE KEY UPDATE
  tenant_id = VALUES(tenant_id),
  product_id = VALUES(product_id),
  platform = VALUES(platform),
  title = VALUES(title),
  url = VALUES(url),
  price_cny = VALUES(price_cny),
  note = VALUES(note);
