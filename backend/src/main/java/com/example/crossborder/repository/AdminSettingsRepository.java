package com.example.crossborder.repository;

import com.example.crossborder.model.AdminSettings;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AdminSettingsRepository {
    public static final String DEFAULT_TENANT = "default";
    private final JdbcTemplate jdbc;
    public AdminSettingsRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public AdminSettings get() { return get(DEFAULT_TENANT); }
    public AdminSettings get(String tenantId) {
        ensureTenantColumn();
        ensureDefault(tenantId);
        return jdbc.queryForObject("SELECT * FROM admin_settings WHERE tenant_id = ? ORDER BY id LIMIT 1", (rs, rowNum) -> new AdminSettings(
            split(rs.getString("foreign_sources")), split(rs.getString("domestic_sources")), split(rs.getString("categories")), split(rs.getString("regions")),
            rs.getString("frequency_cron"), rs.getInt("max_products"), rs.getBigDecimal("jpy_cny_rate"), rs.getBigDecimal("default_shipping_cny"), rs.getBoolean("smart_mode")
        ), tenantId);
    }

    public AdminSettings save(AdminSettings s) { return save(DEFAULT_TENANT, s); }
    public AdminSettings save(String tenantId, AdminSettings s) {
        ensureTenantColumn();
        ensureDefault(tenantId);
        jdbc.update("""
            UPDATE admin_settings SET foreign_sources=?, domestic_sources=?, categories=?, regions=?, frequency_cron=?,
              max_products=?, jpy_cny_rate=?, default_shipping_cny=?, smart_mode=? WHERE tenant_id=?
            """, join(s.foreignSources()), join(s.domesticSources()), join(s.categories()), join(s.regions()),
            blankDefault(s.frequencyCron(), "0 30 8 * * *"), Math.max(1, s.maxProducts()), nonNull(s.jpyCnyRate(), "0.048"),
            nonNull(s.defaultShippingCny(), "18"), s.smartMode(), tenantId);
        return get(tenantId);
    }

    public void ensureDefault(String tenantId) {
        ensureTenantColumn();
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM admin_settings WHERE tenant_id = ?", Integer.class, tenantId);
        if (count == null || count == 0) {
            jdbc.update("""
              INSERT INTO admin_settings(id, tenant_id, foreign_sources, domestic_sources, categories, regions, frequency_cron, max_products, jpy_cny_rate, default_shipping_cny, smart_mode)
              VALUES(?, ?, 'TikTok/Apify,Amazon/Rainforest,Amazon/Keepa', '1688,Taobao,Pinduoduo', '玩具,家居,美妆,宠物,数码,户外,母婴,汽车,厨房,文具,服饰,健康', '日本', '0 30 8 * * *', 30, 0.048, 18, true)
            """, "default".equals(tenantId) ? 1 : Math.abs(tenantId.hashCode()) + 1000L, tenantId);
        }
    }
    private void ensureTenantColumn() {
        Integer exists = jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'admin_settings' AND column_name = 'tenant_id'", Integer.class);
        if (exists == null || exists == 0) {
            jdbc.execute("ALTER TABLE admin_settings ADD COLUMN tenant_id VARCHAR(64) NOT NULL DEFAULT 'default'");
            jdbc.execute("UPDATE admin_settings SET tenant_id='default' WHERE tenant_id IS NULL OR tenant_id='' ");
        }
    }
    private List<String> split(String s) { return Arrays.stream((s == null ? "" : s).split(",")).map(String::trim).filter(x -> !x.isBlank()).collect(Collectors.toList()); }
    private String join(List<String> values) { return values == null ? "" : values.stream().map(String::trim).filter(x -> !x.isBlank()).collect(Collectors.joining(",")); }
    private String blankDefault(String value, String fallback) { return value == null || value.isBlank() ? fallback : value.trim(); }
    private BigDecimal nonNull(BigDecimal value, String fallback) { return value == null ? new BigDecimal(fallback) : value; }
}

