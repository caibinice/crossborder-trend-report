package com.example.crossborder.repository;

import com.example.crossborder.model.AdminMenu;
import com.example.crossborder.model.AdminRole;
import com.example.crossborder.model.AdminUser;
import com.example.crossborder.model.CategoryConfig;
import com.example.crossborder.model.MarketConfig;
import com.example.crossborder.model.ScheduleConfig;
import java.util.ArrayList;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AdminDataRepository {
    private final JdbcTemplate jdbc;
    public AdminDataRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public void ensureSeedData() {
        if (count("admin_roles") == 0) {
            jdbc.update("INSERT INTO admin_roles(role_key,role_name,status,remark) VALUES('admin','超级管理员','enabled','系统内置超级管理员')");
            jdbc.update("INSERT INTO admin_roles(role_key,role_name,status,remark) VALUES('operator','运营人员','enabled','负责选品和报表运营')");
        }
        if (count("admin_users") == 0) {
            jdbc.update("INSERT INTO admin_users(username,nickname,role_key,status,email) VALUES('admin','系统管理员','admin','enabled','admin@example.com')");
        }
        if (count("admin_menus") == 0) seedMenus();
        if (count("market_configs") == 0) {
            jdbc.update("INSERT INTO market_configs(market_key,market_name,region,enabled,note) VALUES('jp','日本市场','日本',true,'已接入 demo 趋势数据，可配置真实 TikTok/Amazon JP 数据源')");
            jdbc.update("INSERT INTO market_configs(market_key,market_name,region,enabled,note) VALUES('us','美国市场','美国',false,'即将接入 Amazon US / TikTok US 数据源')");
            jdbc.update("INSERT INTO market_configs(market_key,market_name,region,enabled,note) VALUES('sea','东南亚市场','东南亚',false,'即将接入 TikTok Shop SEA / Shopee / Lazada 数据源')");
        }
        if (count("category_configs") == 0) {
            for (String c : List.of("玩具","家居","美妆","宠物","数码","户外","母婴","汽车","厨房","文具","服饰","健康")) {
                jdbc.update("INSERT INTO category_configs(category_name,market_key,enabled,keywords,note) VALUES(?,?,?,?,?)", c, "jp", true, c, "默认日本市场品类");
            }
        }
    }

    private int count(String table) {
        Integer c = jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class);
        return c == null ? 0 : c;
    }

    private void seedMenus() {
        addMenu(1, 0, "dashboard", "首页 / 工作台", "dashboard", "/admin", "Dashboard", 1);
        addMenu(2, 0, "system", "系统管理", "system", "/admin/system", "Layout", 10);
        addMenu(3, 2, "users", "用户管理", "user", "/admin/system/users", "UserManage", 11);
        addMenu(4, 2, "roles", "角色管理", "peoples", "/admin/system/roles", "RoleManage", 12);
        addMenu(5, 2, "menus", "菜单管理", "tree-table", "/admin/system/menus", "MenuManage", 13);
        addMenu(6, 0, "selection", "选品配置", "shopping", "/admin/selection", "Layout", 20);
        addMenu(7, 6, "sources", "数据源配置", "link", "/admin/selection/sources", "SourceConfig", 21);
        addMenu(8, 6, "markets", "市场配置", "international", "/admin/selection/markets", "MarketConfig", 22);
        addMenu(9, 6, "categories", "品类配置", "category", "/admin/selection/categories", "CategoryConfig", 23);
        addMenu(10, 6, "schedules", "采集频率配置", "time", "/admin/selection/schedules", "ScheduleConfig", 24);
        addMenu(11, 0, "reports", "报表管理", "chart", "/admin/reports", "Layout", 30);
        addMenu(12, 11, "dailyReports", "日报记录", "date", "/admin/reports/daily", "DailyReports", 31);
        addMenu(13, 11, "productPool", "商品池", "goods", "/admin/reports/products", "ProductPool", 32);
    }
    private void addMenu(long id, long parentId, String key, String title, String icon, String path, String component, int sort) {
        jdbc.update("INSERT INTO admin_menus(id,parent_id,menu_key,title,icon,path,component,sort_order,status) VALUES(?,?,?,?,?,?,?,?,'enabled')", id, parentId, key, title, icon, path, component, sort);
    }

    public List<AdminUser> users() { ensureSeedData(); return jdbc.query("SELECT * FROM admin_users ORDER BY id", (rs,n) -> new AdminUser(rs.getLong("id"), rs.getString("username"), rs.getString("nickname"), rs.getString("role_key"), rs.getString("status"), rs.getString("email"))); }
    public AdminUser saveUser(AdminUser u) { ensureSeedData(); if (u.id() > 0) jdbc.update("UPDATE admin_users SET username=?,nickname=?,role_key=?,status=?,email=? WHERE id=?", u.username(), u.nickname(), u.roleKey(), u.status(), u.email(), u.id()); else jdbc.update("INSERT INTO admin_users(username,nickname,role_key,status,email) VALUES(?,?,?,?,?)", u.username(), u.nickname(), u.roleKey(), u.status(), u.email()); return users().stream().filter(x -> x.username().equals(u.username())).findFirst().orElse(users().get(0)); }
    public void deleteUser(long id) { jdbc.update("DELETE FROM admin_users WHERE id=? AND username <> 'admin'", id); }

    public List<AdminRole> roles() { ensureSeedData(); return jdbc.query("SELECT * FROM admin_roles ORDER BY id", (rs,n) -> new AdminRole(rs.getLong("id"), rs.getString("role_key"), rs.getString("role_name"), rs.getString("status"), rs.getString("remark"))); }
    public AdminRole saveRole(AdminRole r) { ensureSeedData(); if (r.id() > 0) jdbc.update("UPDATE admin_roles SET role_key=?,role_name=?,status=?,remark=? WHERE id=?", r.roleKey(), r.roleName(), r.status(), r.remark(), r.id()); else jdbc.update("INSERT INTO admin_roles(role_key,role_name,status,remark) VALUES(?,?,?,?)", r.roleKey(), r.roleName(), r.status(), r.remark()); return roles().stream().filter(x -> x.roleKey().equals(r.roleKey())).findFirst().orElse(roles().get(0)); }
    public void deleteRole(long id) { jdbc.update("DELETE FROM admin_roles WHERE id=? AND role_key <> 'admin'", id); }

    public List<AdminMenu> menus() { ensureSeedData(); List<AdminMenu> flat = jdbc.query("SELECT * FROM admin_menus ORDER BY sort_order,id", (rs,n) -> new AdminMenu(rs.getLong("id"), rs.getLong("parent_id"), rs.getString("menu_key"), rs.getString("title"), rs.getString("icon"), rs.getString("path"), rs.getString("component"), rs.getInt("sort_order"), rs.getString("status"), new ArrayList<>())); return tree(flat, 0); }
    private List<AdminMenu> tree(List<AdminMenu> flat, long parent) { List<AdminMenu> result = new ArrayList<>(); for (AdminMenu m : flat) if (m.parentId() == parent) result.add(new AdminMenu(m.id(),m.parentId(),m.menuKey(),m.title(),m.icon(),m.path(),m.component(),m.sortOrder(),m.status(),tree(flat,m.id()))); return result; }

    public List<MarketConfig> markets() { ensureSeedData(); return jdbc.query("SELECT * FROM market_configs ORDER BY id", (rs,n) -> new MarketConfig(rs.getLong("id"), rs.getString("market_key"), rs.getString("market_name"), rs.getString("region"), rs.getBoolean("enabled"), rs.getString("note"))); }
    public MarketConfig saveMarket(MarketConfig m) { ensureSeedData(); if (m.id() > 0) jdbc.update("UPDATE market_configs SET market_key=?,market_name=?,region=?,enabled=?,note=? WHERE id=?", m.marketKey(),m.marketName(),m.region(),m.enabled(),m.note(),m.id()); else jdbc.update("INSERT INTO market_configs(market_key,market_name,region,enabled,note) VALUES(?,?,?,?,?)", m.marketKey(),m.marketName(),m.region(),m.enabled(),m.note()); return markets().stream().filter(x -> x.marketKey().equals(m.marketKey())).findFirst().orElse(markets().get(0)); }

    public List<CategoryConfig> categories() { ensureSeedData(); return jdbc.query("SELECT * FROM category_configs ORDER BY id", (rs,n) -> new CategoryConfig(rs.getLong("id"), rs.getString("category_name"), rs.getString("market_key"), rs.getBoolean("enabled"), rs.getString("keywords"), rs.getString("note"))); }
    public CategoryConfig saveCategory(CategoryConfig c) { ensureSeedData(); if (c.id() > 0) jdbc.update("UPDATE category_configs SET category_name=?,market_key=?,enabled=?,keywords=?,note=? WHERE id=?", c.categoryName(),c.marketKey(),c.enabled(),c.keywords(),c.note(),c.id()); else jdbc.update("INSERT INTO category_configs(category_name,market_key,enabled,keywords,note) VALUES(?,?,?,?,?)", c.categoryName(),c.marketKey(),c.enabled(),c.keywords(),c.note()); return categories().stream().filter(x -> x.categoryName().equals(c.categoryName())).findFirst().orElse(categories().get(0)); }

    public ScheduleConfig schedule() { ensureSeedData(); return jdbc.queryForObject("SELECT frequency_cron,max_products,jpy_cny_rate,default_shipping_cny,smart_mode FROM admin_settings WHERE id=1", (rs,n) -> new ScheduleConfig(rs.getString("frequency_cron"), rs.getInt("max_products"), rs.getBigDecimal("jpy_cny_rate"), rs.getBigDecimal("default_shipping_cny"), rs.getBoolean("smart_mode"))); }
}
