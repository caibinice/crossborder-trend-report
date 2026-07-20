package com.example.crossborder.repository;

import com.example.crossborder.model.AdminSettings;
import com.example.crossborder.model.SupplierSiteConfig;
import com.example.crossborder.service.ApiConflictException;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AdminSettingsRepository {
    public static final String DEFAULT_TENANT = "default";
    private final JdbcTemplate jdbc;

    public AdminSettingsRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public AdminSettings get() {
        return get(DEFAULT_TENANT);
    }

    public AdminSettings get(String tenantId) {
        return jdbc.query(
            "SELECT * FROM admin_settings WHERE tenant_id = ? ORDER BY id LIMIT 1",
            (rs, rowNum) -> new AdminSettings(
                split(s(rs, "foreign_sources")),
                split(s(rs, "domestic_sources")),
                split(s(rs, "categories")),
                split(s(rs, "regions")),
                s(rs, "source_mode"),
                s(rs, "frequency_cron"),
                rs.getInt("max_products"),
                rs.getBigDecimal("jpy_cny_rate"),
                rs.getBoolean("auto_exchange_rate"),
                rs.getBigDecimal("default_shipping_cny"),
                rs.getBoolean("smart_mode"),
                rs.getInt("max_categories"),
                rs.getInt("products_per_category"),
                s(rs, "ranking_metric"),
                supplierSites(s(rs, "supplier_sites"))
            ),
            tenantId
        ).stream().findFirst().orElseThrow(() -> new ApiConflictException("租户尚未完成系统配置初始化"));
    }

    public AdminSettings save(AdminSettings s) {
        return save(DEFAULT_TENANT, s);
    }

    public AdminSettings save(String tenantId, AdminSettings s) {
        int updated = jdbc.update("""
            UPDATE admin_settings SET foreign_sources=?, domestic_sources=?, supplier_sites=?, categories=?, regions=?, source_mode=?, frequency_cron=?,
              max_products=?, max_categories=?, products_per_category=?, ranking_metric=?, jpy_cny_rate=?, auto_exchange_rate=?,
              default_shipping_cny=?, smart_mode=? WHERE tenant_id=?
            """,
            join(s.foreignSources()),
            join(domesticSources(s)),
            joinSupplierSites(s.supplierSites()),
            join(s.categories()),
            join(s.regions()),
            sourceMode(s.sourceMode()),
            blankDefault(s.frequencyCron(), "0 30 8 * * *"),
            Math.min(500, Math.max(s.maxProducts(), safeProductLimit(s))),
            Math.max(1, s.maxCategories()),
            Math.max(1, s.productsPerCategory()),
            rankingMetric(s.rankingMetric()),
            nonNull(s.jpyCnyRate(), "0.048"),
            s.autoExchangeRate(),
            nonNull(s.defaultShippingCny(), "18"),
            s.smartMode(),
            tenantId
        );
        if (updated == 0) {
            throw new ApiConflictException("租户尚未完成系统配置初始化");
        }
        return get(tenantId);
    }

    /** Called only by explicit bootstrap or tenant creation, never from a read request. */
    public void createDefaultIfMissing(String tenantId) {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM admin_settings WHERE tenant_id = ?", Integer.class, tenantId);
        if (count == null || count == 0) {
            Long nextId = jdbc.queryForObject("SELECT COALESCE(MAX(id), 0) + 1 FROM admin_settings", Long.class);
            jdbc.update("""
                INSERT INTO admin_settings(
                  id, tenant_id, foreign_sources, domestic_sources, categories, regions, source_mode,
                  frequency_cron, max_products, jpy_cny_rate, auto_exchange_rate, default_shipping_cny, smart_mode
                ) VALUES(
                  ?, ?, 'WooCommerce公开目录,Google Trends,Yahoo Shopping,Rakuten', '1688,Taobao,Pinduoduo',
                  '玩具,家居,美妆,宠物,数码,户外,母婴,厨房,服饰,食品',
                  '日本', 'external', '0 30 8 * * *', 100, 0.048, true, 18, true
                )
                """,
                nextId == null ? 1L : nextId,
                tenantId
            );
        }
    }

    private List<String> split(String s) {
        return Arrays.stream((s == null ? "" : s).split(","))
            .map(String::trim)
            .filter(x -> !x.isBlank())
            .collect(Collectors.toList());
    }

    private String join(List<String> values) {
        return values == null ? "" : values.stream().map(String::trim).filter(x -> !x.isBlank()).collect(Collectors.joining(","));
    }

    private String blankDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private BigDecimal nonNull(BigDecimal value, String fallback) {
        return value == null ? new BigDecimal(fallback) : value;
    }

    private String sourceMode(String value) {
        return switch (value == null ? "" : value.trim().toLowerCase()) {
            case "demo", "mixed" -> value.trim().toLowerCase();
            default -> "external";
        };
    }

    private String rankingMetric(String value) {
        return "sales_amount".equals(value) ? "sales_amount" : "sales_volume";
    }

    private int safeProductLimit(AdminSettings settings) {
        return Math.max(1, settings.maxCategories()) * Math.max(1, settings.productsPerCategory());
    }

    private List<String> domesticSources(AdminSettings settings) {
        if (settings.supplierSites() != null && !settings.supplierSites().isEmpty()) {
            return settings.supplierSites().stream().map(SupplierSiteConfig::name).toList();
        }
        return settings.domesticSources();
    }

    private List<SupplierSiteConfig> supplierSites(String value) {
        String configured = value == null || value.isBlank()
            ? "1688|https://s.1688.com/selloffer/offer_search.htm?keywords={keyword}\n"
                + "淘宝|https://s.taobao.com/search?q={keyword}\n"
                + "拼多多|https://mobile.yangkeduo.com/search_result.html?search_key={keyword}"
            : value;
        return configured.lines()
            .map(String::trim)
            .filter(line -> !line.isBlank() && line.contains("|"))
            .map(line -> line.split("\\|", 2))
            .map(parts -> new SupplierSiteConfig(parts[0].trim(), parts[1].trim()))
            .filter(site -> !site.name().isBlank() && !site.urlTemplate().isBlank())
            .toList();
    }

    private String joinSupplierSites(List<SupplierSiteConfig> sites) {
        if (sites == null) return "";
        return sites.stream()
            .filter(site -> site != null && site.name() != null && site.urlTemplate() != null)
            .map(site -> site.name().trim() + "|" + site.urlTemplate().trim())
            .filter(line -> !line.equals("|"))
            .collect(Collectors.joining("\n"));
    }

    private String s(ResultSet rs, String column) throws SQLException {
        return rs.getString(column);
    }
}
