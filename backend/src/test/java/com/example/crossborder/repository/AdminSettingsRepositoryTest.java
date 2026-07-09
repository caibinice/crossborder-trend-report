package com.example.crossborder.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;

import com.example.crossborder.model.AdminSettings;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class AdminSettingsRepositoryTest {
    private JdbcTemplate jdbcTemplate;
    private AdminSettingsRepository repository;

    @BeforeEach
    void setUp() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUrl("jdbc:h2:mem:admin_settings_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
        dataSource.setUsername("sa");
        dataSource.setPassword("");

        jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.execute("DROP TABLE IF EXISTS admin_settings");
        jdbcTemplate.execute("""
            CREATE TABLE admin_settings (
              id BIGINT PRIMARY KEY,
              foreign_sources VARCHAR(512) NOT NULL,
              domestic_sources VARCHAR(512) NOT NULL,
              categories VARCHAR(1024) NOT NULL,
              regions VARCHAR(512) NOT NULL,
              frequency_cron VARCHAR(64) NOT NULL,
              max_products INT NOT NULL,
              jpy_cny_rate DECIMAL(10,6) NOT NULL,
              default_shipping_cny DECIMAL(12,2) NOT NULL,
              smart_mode BOOLEAN NOT NULL
            )
            """);

        repository = new AdminSettingsRepository(jdbcTemplate);
    }

    @Test
    void get_shouldSeedReadableChineseDefaults() {
        repository.ensureDefault("default");
        AdminSettings settings = jdbcTemplate.queryForObject(
            "SELECT * FROM admin_settings WHERE tenant_id = ?",
            (rs, rowNum) -> new AdminSettings(
                List.of(rs.getString("foreign_sources").split(",")),
                List.of(rs.getString("domestic_sources").split(",")),
                List.of(rs.getString("categories").split(",")),
                List.of(rs.getString("regions").split(",")),
                rs.getString("frequency_cron"),
                rs.getInt("max_products"),
                rs.getBigDecimal("jpy_cny_rate"),
                rs.getBigDecimal("default_shipping_cny"),
                rs.getBoolean("smart_mode")
            ),
            "default"
        );

        assertIterableEquals(List.of("TikTok/Apify", "Amazon/Rainforest", "Amazon/Keepa"), settings.foreignSources());
        assertIterableEquals(List.of("1688", "Taobao", "Pinduoduo"), settings.domesticSources());
        assertIterableEquals(List.of("玩具", "家居", "美妆", "宠物", "数码", "户外", "母婴", "汽车", "厨房", "文具", "服饰", "健康"), settings.categories());
        assertIterableEquals(List.of("日本"), settings.regions());
        assertEquals("0 30 8 * * *", settings.frequencyCron());
        assertEquals(30, settings.maxProducts());
        assertEquals(new BigDecimal("0.048000"), settings.jpyCnyRate());
        assertEquals(new BigDecimal("18.00"), settings.defaultShippingCny());
    }
}
