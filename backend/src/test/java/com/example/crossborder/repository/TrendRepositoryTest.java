package com.example.crossborder.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.example.crossborder.model.TrendReport;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.AbstractDataSource;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class TrendRepositoryTest {
    private JdbcTemplate jdbc;
    private AtomicInteger preparedStatements;
    private TrendRepository repository;

    @BeforeEach
    void setUp() {
        DriverManagerDataSource delegate = new DriverManagerDataSource();
        delegate.setDriverClassName("org.h2.Driver");
        delegate.setUrl("jdbc:h2:mem:trend_repository_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
        delegate.setUsername("sa");
        delegate.setPassword("");
        preparedStatements = new AtomicInteger();
        jdbc = new JdbcTemplate(new CountingDataSource(delegate, preparedStatements));

        jdbc.execute("DROP TABLE IF EXISTS domestic_links");
        jdbc.execute("DROP TABLE IF EXISTS trend_products");
        jdbc.execute("DROP TABLE IF EXISTS trend_reports");
        jdbc.execute("""
            CREATE TABLE trend_reports (
              id BIGINT PRIMARY KEY,
              tenant_id VARCHAR(64) NOT NULL,
              report_date DATE NOT NULL,
              source_mode VARCHAR(255) NOT NULL,
              source_key VARCHAR(128) NOT NULL,
              title VARCHAR(255) NOT NULL,
              summary VARCHAR(1024),
              created_at TIMESTAMP NOT NULL
            )
            """);
        jdbc.execute("""
            CREATE TABLE trend_products (
              id BIGINT PRIMARY KEY,
              tenant_id VARCHAR(64) NOT NULL,
              report_id BIGINT NOT NULL,
              product_rank INT NOT NULL,
              category VARCHAR(64) NOT NULL,
              product_name_jp VARCHAR(255) NOT NULL,
              product_name_cn VARCHAR(255) NOT NULL,
              keywords VARCHAR(255) NOT NULL,
              source_platform VARCHAR(64) NOT NULL,
              source_url VARCHAR(1024),
              image_url VARCHAR(1024),
              heat_score DOUBLE NOT NULL,
              source_price DECIMAL(14,4),
              source_currency VARCHAR(3),
              source_price_cny DECIMAL(14,2),
              jp_price_jpy DECIMAL(12,2),
              jp_price_cny DECIMAL(12,2),
              domestic_cost_cny DECIMAL(12,2),
              shipping_cny DECIMAL(12,2),
              estimated_profit_cny DECIMAL(12,2),
              estimated_margin DOUBLE,
              reason VARCHAR(1024)
            )
            """);
        jdbc.execute("""
            CREATE TABLE domestic_links (
              id BIGINT PRIMARY KEY,
              tenant_id VARCHAR(64) NOT NULL,
              product_id BIGINT NOT NULL,
              platform VARCHAR(64) NOT NULL,
              title VARCHAR(255) NOT NULL,
              url VARCHAR(1024) NOT NULL,
              price_cny DECIMAL(12,2),
              note VARCHAR(255)
            )
            """);

        jdbc.update("INSERT INTO trend_reports VALUES(1,'default',DATE '2026-07-15','演示','jp:demo','日报一','摘要一',TIMESTAMP '2026-07-15 08:00:00')");
        jdbc.update("INSERT INTO trend_reports VALUES(2,'default',DATE '2026-07-14','演示','jp:demo','日报二','摘要二',TIMESTAMP '2026-07-14 08:00:00')");
        insertProduct(101, 1, 1, "产品 A");
        insertProduct(102, 1, 2, "产品 B");
        insertProduct(201, 2, 1, "产品 C");
        jdbc.update("INSERT INTO domestic_links VALUES(1,'default',101,'1688','供应商 A','https://example.com/a',12.34,'备注')");
        jdbc.update("INSERT INTO domestic_links VALUES(2,'default',201,'1688','供应商 C','https://example.com/c',23.45,'备注')");
        repository = new TrendRepository(jdbc);
    }

    @Test
    void list_loadsAllDetailsWithThreeBatchedQueries() {
        preparedStatements.set(0);

        List<TrendReport> reports = repository.list();

        assertEquals(2, reports.size());
        assertEquals(2, reports.get(0).products().size());
        assertEquals(1, reports.get(0).products().get(0).domesticLinks().size());
        assertEquals(1, reports.get(1).products().size());
        assertEquals(1, reports.get(1).products().get(0).domesticLinks().size());
        assertEquals(3, preparedStatements.get(), "日报、商品和货源链接应分别仅查询一次");
    }

    private void insertProduct(long id, long reportId, int rank, String name) {
        jdbc.update("""
            INSERT INTO trend_products(
              id,tenant_id,report_id,product_rank,category,product_name_jp,product_name_cn,keywords,
              source_platform,source_url,image_url,heat_score,source_price,source_currency,source_price_cny,
              jp_price_jpy,jp_price_cny,domestic_cost_cny,shipping_cny,estimated_profit_cny,estimated_margin,reason
            ) VALUES(?, 'default', ?, ?, '玩具', ?, ?, '关键词', 'demo', 'https://example.com', null, 90,
              100, 'JPY', 5, 100, 5, 2, 1, 2, 0.4, '原因')
            """, id, reportId, rank, name + " 日文", name);
    }

    private static class CountingDataSource extends AbstractDataSource {
        private final DataSource delegate;
        private final AtomicInteger preparedStatements;

        private CountingDataSource(DataSource delegate, AtomicInteger preparedStatements) {
            this.delegate = delegate;
            this.preparedStatements = preparedStatements;
        }

        @Override
        public Connection getConnection() throws SQLException {
            return wrap(delegate.getConnection());
        }

        @Override
        public Connection getConnection(String username, String password) throws SQLException {
            return wrap(delegate.getConnection(username, password));
        }

        private Connection wrap(Connection connection) {
            return (Connection) Proxy.newProxyInstance(
                connection.getClass().getClassLoader(),
                new Class<?>[]{Connection.class},
                (proxy, method, arguments) -> {
                    if ("prepareStatement".equals(method.getName())) {
                        preparedStatements.incrementAndGet();
                    }
                    try {
                        return method.invoke(connection, arguments);
                    } catch (InvocationTargetException exception) {
                        throw exception.getCause();
                    }
                }
            );
        }
    }
}
