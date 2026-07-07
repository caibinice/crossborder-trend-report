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
    private final JdbcTemplate jdbc;
    public AdminSettingsRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public AdminSettings get() {
        ensureDefault();
        return jdbc.queryForObject("SELECT * FROM admin_settings WHERE id = 1", (rs, rowNum) -> new AdminSettings(
            split(rs.getString("foreign_sources")),
            split(rs.getString("domestic_sources")),
            split(rs.getString("categories")),
            split(rs.getString("regions")),
            rs.getString("frequency_cron"),
            rs.getInt("max_products"),
            rs.getBigDecimal("jpy_cny_rate"),
            rs.getBigDecimal("default_shipping_cny"),
            rs.getBoolean("smart_mode")
        ));
    }

    public AdminSettings save(AdminSettings s) {
        jdbc.update("""
            UPDATE admin_settings SET foreign_sources=?, domestic_sources=?, categories=?, regions=?, frequency_cron=?,
              max_products=?, jpy_cny_rate=?, default_shipping_cny=?, smart_mode=? WHERE id=1
            """, join(s.foreignSources()), join(s.domesticSources()), join(s.categories()), join(s.regions()),
            blankDefault(s.frequencyCron(), "0 30 8 * * *"), Math.max(1, s.maxProducts()), nonNull(s.jpyCnyRate(), "0.048"),
            nonNull(s.defaultShippingCny(), "18"), s.smartMode());
        return get();
    }

    private void ensureDefault() {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM admin_settings WHERE id = 1", Integer.class);
        if (count == null || count == 0) {
            jdbc.update("""
              INSERT INTO admin_settings(id, foreign_sources, domestic_sources, categories, regions, frequency_cron, max_products, jpy_cny_rate, default_shipping_cny, smart_mode)
              VALUES(1, 'TikTok/Apify,Amazon/Rainforest,Amazon/Keepa', '1688,Taobao,Pinduoduo', '玩具,家居,美妆,宠物,数码,户外,母婴,汽车,厨房,文具,服饰,健康', '日本', '0 30 8 * * *', 30, 0.048, 18, true)
            """);
        }
    }
    private List<String> split(String s) { return Arrays.stream((s == null ? "" : s).split(",")).map(String::trim).filter(x -> !x.isBlank()).collect(Collectors.toList()); }
    private String join(List<String> values) { return values == null ? "" : values.stream().map(String::trim).filter(x -> !x.isBlank()).collect(Collectors.joining(",")); }
    private String blankDefault(String value, String fallback) { return value == null || value.isBlank() ? fallback : value.trim(); }
    private BigDecimal nonNull(BigDecimal value, String fallback) { return value == null ? new BigDecimal(fallback) : value; }
}
