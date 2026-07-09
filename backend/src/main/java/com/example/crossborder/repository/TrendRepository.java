package com.example.crossborder.repository;

import com.example.crossborder.model.DomesticLink;
import com.example.crossborder.model.TrendProduct;
import com.example.crossborder.model.TrendReport;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class TrendRepository {
    private final JdbcTemplate jdbc;

    public TrendRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional
    public long createReport(LocalDate date, String mode, String title, String summary) {
        jdbc.update("DELETE FROM trend_reports WHERE report_date=? AND source_mode=?", java.sql.Date.valueOf(date), mode);
        KeyHolder kh = new GeneratedKeyHolder();
        jdbc.update(c -> {
            PreparedStatement ps = c.prepareStatement(
                "INSERT INTO trend_reports(report_date,source_mode,title,summary) VALUES(?,?,?,?)",
                Statement.RETURN_GENERATED_KEYS
            );
            ps.setDate(1, java.sql.Date.valueOf(date));
            ps.setString(2, mode);
            ps.setString(3, title);
            ps.setString(4, summary);
            return ps;
        }, kh);
        return kh.getKey().longValue();
    }

    public long addProduct(TrendProduct p) {
        KeyHolder kh = new GeneratedKeyHolder();
        jdbc.update(c -> {
            PreparedStatement ps = c.prepareStatement("""
                INSERT INTO trend_products(
                  report_id,product_rank,category,product_name_jp,product_name_cn,keywords,
                  source_platform,source_url,heat_score,jp_price_jpy,jp_price_cny,domestic_cost_cny,
                  shipping_cny,estimated_profit_cny,estimated_margin,reason
                ) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                """, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, p.reportId());
            ps.setInt(2, p.rank());
            ps.setString(3, p.category());
            ps.setString(4, p.productNameJp());
            ps.setString(5, p.productNameCn());
            ps.setString(6, p.keywords());
            ps.setString(7, p.sourcePlatform());
            ps.setString(8, p.sourceUrl());
            ps.setDouble(9, p.heatScore());
            ps.setBigDecimal(10, p.jpPriceJpy());
            ps.setBigDecimal(11, p.jpPriceCny());
            ps.setBigDecimal(12, p.domesticCostCny());
            ps.setBigDecimal(13, p.shippingCny());
            ps.setBigDecimal(14, p.estimatedProfitCny());
            ps.setDouble(15, p.estimatedMargin());
            ps.setString(16, p.reason());
            return ps;
        }, kh);
        long id = kh.getKey().longValue();
        for (DomesticLink l : p.domesticLinks()) {
            addLink(id, l);
        }
        return id;
    }

    private void addLink(long pid, DomesticLink l) {
        jdbc.update(
            "INSERT INTO domestic_links(product_id,platform,title,url,price_cny,note) VALUES(?,?,?,?,?,?)",
            pid, l.platform(), l.title(), l.url(), l.priceCny(), l.note()
        );
    }

    public Optional<TrendReport> latest() {
        List<Long> ids = jdbc.query(
            "SELECT id FROM trend_reports ORDER BY report_date DESC,created_at DESC LIMIT 1",
            (rs, n) -> rs.getLong(1)
        );
        return ids.stream().findFirst().map(this::mapReport);
    }

    public Optional<TrendReport> byDate(LocalDate d) {
        List<Long> ids = jdbc.query(
            "SELECT id FROM trend_reports WHERE report_date=? ORDER BY created_at DESC LIMIT 1",
            (rs, n) -> rs.getLong(1),
            java.sql.Date.valueOf(d)
        );
        return ids.stream().findFirst().map(this::mapReport);
    }

    public List<TrendReport> list() {
        return jdbc.query(
            "SELECT id FROM trend_reports ORDER BY report_date DESC,created_at DESC LIMIT 30",
            (rs, n) -> mapReport(rs.getLong(1))
        );
    }

    public int countReports() {
        Integer c = jdbc.queryForObject("SELECT COUNT(*) FROM trend_reports", Integer.class);
        return c == null ? 0 : c;
    }

    public int countProducts() {
        Integer c = jdbc.queryForObject("SELECT COUNT(*) FROM trend_products", Integer.class);
        return c == null ? 0 : c;
    }

    private TrendReport mapReport(long id) {
        return jdbc.queryForObject(
            "SELECT * FROM trend_reports WHERE id=?",
            (rs, n) -> new TrendReport(
                rs.getLong("id"),
                rs.getDate("report_date").toLocalDate(),
                s(rs, "source_mode"),
                s(rs, "title"),
                s(rs, "summary"),
                ts(rs.getTimestamp("created_at")),
                products(id)
            ),
            id
        );
    }

    private List<TrendProduct> products(long rid) {
        return jdbc.query(
            "SELECT * FROM trend_products WHERE report_id=? ORDER BY product_rank",
            (rs, n) -> {
                long pid = rs.getLong("id");
                return new TrendProduct(
                    pid,
                    rs.getLong("report_id"),
                    rs.getInt("product_rank"),
                    s(rs, "category"),
                    s(rs, "product_name_jp"),
                    s(rs, "product_name_cn"),
                    s(rs, "keywords"),
                    s(rs, "source_platform"),
                    s(rs, "source_url"),
                    rs.getDouble("heat_score"),
                    rs.getBigDecimal("jp_price_jpy"),
                    rs.getBigDecimal("jp_price_cny"),
                    rs.getBigDecimal("domestic_cost_cny"),
                    rs.getBigDecimal("shipping_cny"),
                    rs.getBigDecimal("estimated_profit_cny"),
                    rs.getDouble("estimated_margin"),
                    s(rs, "reason"),
                    links(pid)
                );
            },
            rid
        );
    }

    private List<DomesticLink> links(long pid) {
        return jdbc.query(
            "SELECT * FROM domestic_links WHERE product_id=? ORDER BY price_cny",
            (rs, n) -> new DomesticLink(
                rs.getLong("id"),
                rs.getLong("product_id"),
                s(rs, "platform"),
                s(rs, "title"),
                s(rs, "url"),
                rs.getBigDecimal("price_cny"),
                s(rs, "note")
            ),
            pid
        );
    }

    private String s(ResultSet rs, String column) throws SQLException {
        return rs.getString(column);
    }

    private Instant ts(Timestamp t) {
        return t == null ? Instant.EPOCH : t.toInstant();
    }
}
