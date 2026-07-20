package com.example.crossborder.repository;

import com.example.crossborder.model.DomesticLink;
import com.example.crossborder.model.TrendProduct;
import com.example.crossborder.model.TrendReport;
import com.example.crossborder.model.TrendReportSummary;
import com.example.crossborder.util.SupplierSearchUrlCodec;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class TrendRepository {
    private static final String DEFAULT_TENANT = "default";

    private final JdbcTemplate jdbc;

    public TrendRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Serializes collection for a report key across application instances. Must run inside a transaction. */
    public void lockForCollection(LocalDate date, String sourceKey) {
        jdbc.update(
            "INSERT INTO report_collection_locks(tenant_id,report_date,source_key) VALUES(?,?,?) ON DUPLICATE KEY UPDATE locked_at=locked_at",
            DEFAULT_TENANT, java.sql.Date.valueOf(date), sourceKey
        );
        jdbc.queryForObject(
            "SELECT source_key FROM report_collection_locks WHERE tenant_id=? AND report_date=? AND source_key=? FOR UPDATE",
            String.class, DEFAULT_TENANT, java.sql.Date.valueOf(date), sourceKey
        );
    }

    public long createReport(LocalDate date, String sourceKey, String mode, String title, String summary) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO trend_reports(tenant_id,report_date,source_key,source_mode,title,summary) VALUES(?,?,?,?,?,?)",
                Statement.RETURN_GENERATED_KEYS
            );
            statement.setString(1, DEFAULT_TENANT);
            statement.setDate(2, java.sql.Date.valueOf(date));
            statement.setString(3, sourceKey);
            statement.setString(4, mode);
            statement.setString(5, title);
            statement.setString(6, summary);
            return statement;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }

    public void deleteByDateAndSourceKey(LocalDate date, String sourceKey) {
        jdbc.update(
            "DELETE FROM trend_reports WHERE tenant_id=? AND report_date=? AND source_key=?",
            DEFAULT_TENANT, java.sql.Date.valueOf(date), sourceKey
        );
    }

    public long addProduct(TrendProduct product) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO trend_products(
                  tenant_id,report_id,product_rank,category,product_name_jp,product_name_cn,keywords,
                  source_platform,source_url,image_url,heat_score,sales_volume_score,sales_amount_score,ai_score,
                  source_price,source_currency,source_price_cny,
                  jp_price_jpy,jp_price_cny,domestic_cost_cny,shipping_cny,estimated_profit_cny,estimated_margin,reason
                ) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                """, Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, DEFAULT_TENANT);
            statement.setLong(2, product.reportId());
            statement.setInt(3, product.rank());
            statement.setString(4, product.category());
            statement.setString(5, product.productNameJp());
            statement.setString(6, product.productNameCn());
            statement.setString(7, product.keywords());
            statement.setString(8, product.sourcePlatform());
            statement.setString(9, product.sourceUrl());
            statement.setString(10, product.imageUrl());
            statement.setDouble(11, product.heatScore());
            statement.setDouble(12, product.salesVolumeScore());
            statement.setDouble(13, product.salesAmountScore());
            statement.setDouble(14, product.aiScore());
            statement.setBigDecimal(15, product.sourcePrice());
            statement.setString(16, product.sourceCurrency());
            statement.setBigDecimal(17, product.sourcePriceCny());
            statement.setBigDecimal(18, product.jpPriceJpy());
            statement.setBigDecimal(19, product.sourcePriceCny());
            statement.setBigDecimal(20, product.domesticCostCny());
            statement.setBigDecimal(21, product.shippingCny());
            statement.setBigDecimal(22, product.estimatedProfitCny());
            statement.setDouble(23, product.estimatedMargin());
            statement.setString(24, product.reason());
            return statement;
        }, keyHolder);
        long productId = keyHolder.getKey().longValue();
        for (DomesticLink link : product.domesticLinks()) {
            addLink(productId, link);
        }
        return productId;
    }

    private void addLink(long productId, DomesticLink link) {
        jdbc.update(
            "INSERT INTO domestic_links(tenant_id,product_id,platform,title,url,price_cny,note) VALUES(?,?,?,?,?,?,?)",
            DEFAULT_TENANT, productId, link.platform(), link.title(), link.url(), link.priceCny(), link.note()
        );
    }

    public Optional<TrendReport> latest() {
        return jdbc.query(
            "SELECT id FROM trend_reports WHERE tenant_id=? ORDER BY report_date DESC,created_at DESC LIMIT 1",
            (rs, rowNum) -> rs.getLong(1), DEFAULT_TENANT
        ).stream().findFirst().flatMap(this::byId);
    }

    public Optional<TrendReport> byDate(LocalDate date) {
        return jdbc.query(
            "SELECT id FROM trend_reports WHERE tenant_id=? AND report_date=? ORDER BY created_at DESC LIMIT 1",
            (rs, rowNum) -> rs.getLong(1), DEFAULT_TENANT, java.sql.Date.valueOf(date)
        ).stream().findFirst().flatMap(this::byId);
    }

    public Optional<TrendReport> byDateAndSourceKey(LocalDate date, String sourceKey) {
        return jdbc.query(
            "SELECT id FROM trend_reports WHERE tenant_id=? AND report_date=? AND source_key=? ORDER BY created_at DESC LIMIT 1",
            (rs, rowNum) -> rs.getLong(1), DEFAULT_TENANT, java.sql.Date.valueOf(date), sourceKey
        ).stream().findFirst().flatMap(this::byId);
    }

    public Optional<TrendReport> byId(long id) {
        List<ReportRow> reports = jdbc.query(
            "SELECT * FROM trend_reports WHERE id=? AND tenant_id=?",
            reportMapper(), id, DEFAULT_TENANT
        );
        return mapReports(reports).stream().findFirst();
    }

    /** Legacy detail list. Details are loaded in three batched queries rather than N+1 queries. */
    public List<TrendReport> list() {
        List<ReportRow> reports = jdbc.query(
            "SELECT * FROM trend_reports WHERE tenant_id=? ORDER BY report_date DESC,created_at DESC LIMIT 30",
            reportMapper(), DEFAULT_TENANT
        );
        return mapReports(reports);
    }

    public List<TrendReportSummary> listSummaries(int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 100);
        return jdbc.query("""
            SELECT r.id,r.report_date,r.source_mode,r.title,r.summary,r.created_at,COUNT(p.id) AS product_count
            FROM trend_reports r
            LEFT JOIN trend_products p ON p.report_id=r.id
            WHERE r.tenant_id=?
            GROUP BY r.id,r.report_date,r.source_mode,r.title,r.summary,r.created_at
            ORDER BY r.report_date DESC,r.created_at DESC
            LIMIT ?
            """, (rs, rowNum) -> new TrendReportSummary(
                rs.getLong("id"),
                rs.getDate("report_date").toLocalDate(),
                s(rs, "source_mode"),
                s(rs, "title"),
                s(rs, "summary"),
                ts(rs.getTimestamp("created_at")),
                rs.getInt("product_count")
            ), DEFAULT_TENANT, safeLimit);
    }

    public int countReports() {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM trend_reports WHERE tenant_id=?", Integer.class, DEFAULT_TENANT);
        return count == null ? 0 : count;
    }

    public int countProducts() {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM trend_products WHERE tenant_id=?", Integer.class, DEFAULT_TENANT);
        return count == null ? 0 : count;
    }

    private List<TrendReport> mapReports(List<ReportRow> reports) {
        if (reports.isEmpty()) {
            return List.of();
        }
        List<ProductRow> products = productRowsByReport(reports.stream().map(ReportRow::id).toList());
        Map<Long, List<ProductRow>> productsByReport = new HashMap<>();
        for (ProductRow product : products) {
            productsByReport.computeIfAbsent(product.reportId(), ignored -> new ArrayList<>()).add(product);
        }
        Map<Long, List<DomesticLink>> links = linksByProduct(products.stream().map(ProductRow::id).toList());
        return reports.stream().map(report -> new TrendReport(
            report.id(),
            report.reportDate(),
            report.sourceMode(),
            report.title(),
            report.summary(),
            report.createdAt(),
            productsByReport.getOrDefault(report.id(), List.of()).stream().map(product -> mapProduct(product, links)).toList()
        )).toList();
    }

    private List<ProductRow> productRowsByReport(List<Long> reportIds) {
        String placeholders = String.join(",", Collections.nCopies(reportIds.size(), "?"));
        return jdbc.query(
            "SELECT * FROM trend_products WHERE report_id IN (" + placeholders + ") AND tenant_id=? ORDER BY report_id,product_rank",
            productMapper(), appendTenant(reportIds)
        );
    }

    private TrendProduct mapProduct(ProductRow row, Map<Long, List<DomesticLink>> links) {
        return new TrendProduct(
            row.id(), row.reportId(), row.rank(), row.category(), row.productNameJp(), row.productNameCn(), row.keywords(),
            row.sourcePlatform(), row.sourceUrl(), row.imageUrl(), row.heatScore(), row.salesVolumeScore(),
            row.salesAmountScore(), row.aiScore(), row.sourcePrice(), row.sourceCurrency(),
            row.sourcePriceCny(), row.domesticCostCny(), row.shippingCny(), row.estimatedProfitCny(), row.estimatedMargin(),
            row.reason(), links.getOrDefault(row.id(), List.of())
        );
    }

    private Map<Long, List<DomesticLink>> linksByProduct(List<Long> productIds) {
        if (productIds.isEmpty()) {
            return Map.of();
        }
        String placeholders = String.join(",", Collections.nCopies(productIds.size(), "?"));
        List<DomesticLink> links = jdbc.query(
            "SELECT * FROM domestic_links WHERE product_id IN (" + placeholders + ") AND tenant_id=? ORDER BY price_cny",
            (rs, rowNum) -> SupplierSearchUrlCodec.normalizeHistorical(new DomesticLink(
                rs.getLong("id"), rs.getLong("product_id"), s(rs, "platform"), s(rs, "title"),
                s(rs, "url"), rs.getBigDecimal("price_cny"), s(rs, "note")
            )),
            appendTenant(productIds)
        );
        Map<Long, List<DomesticLink>> grouped = new HashMap<>();
        for (DomesticLink link : links) {
            grouped.computeIfAbsent(link.productId(), ignored -> new ArrayList<>()).add(link);
        }
        return grouped;
    }

    private Object[] appendTenant(List<Long> ids) {
        Object[] args = new Object[ids.size() + 1];
        for (int index = 0; index < ids.size(); index++) {
            args[index] = ids.get(index);
        }
        args[ids.size()] = DEFAULT_TENANT;
        return args;
    }

    private record ProductRow(
        long id, long reportId, int rank, String category, String productNameJp, String productNameCn, String keywords,
        String sourcePlatform, String sourceUrl, String imageUrl, double heatScore, double salesVolumeScore,
        double salesAmountScore, double aiScore, java.math.BigDecimal sourcePrice,
        String sourceCurrency, java.math.BigDecimal sourcePriceCny, java.math.BigDecimal domesticCostCny, java.math.BigDecimal shippingCny,
        java.math.BigDecimal estimatedProfitCny, double estimatedMargin, String reason
    ) {}

    private record ReportRow(long id, LocalDate reportDate, String sourceMode, String title, String summary, Instant createdAt) {}

    private org.springframework.jdbc.core.RowMapper<ReportRow> reportMapper() {
        return (rs, rowNum) -> new ReportRow(
            rs.getLong("id"), rs.getDate("report_date").toLocalDate(), s(rs, "source_mode"),
            s(rs, "title"), s(rs, "summary"), ts(rs.getTimestamp("created_at"))
        );
    }

    private org.springframework.jdbc.core.RowMapper<ProductRow> productMapper() {
        return (rs, rowNum) -> new ProductRow(
            rs.getLong("id"), rs.getLong("report_id"), rs.getInt("product_rank"), s(rs, "category"),
            s(rs, "product_name_jp"), s(rs, "product_name_cn"), s(rs, "keywords"), s(rs, "source_platform"),
            s(rs, "source_url"), s(rs, "image_url"), rs.getDouble("heat_score"), rs.getDouble("sales_volume_score"),
            rs.getDouble("sales_amount_score"), rs.getDouble("ai_score"), rs.getBigDecimal("source_price"),
            s(rs, "source_currency"), rs.getBigDecimal("source_price_cny"), rs.getBigDecimal("domestic_cost_cny"), rs.getBigDecimal("shipping_cny"),
            rs.getBigDecimal("estimated_profit_cny"), rs.getDouble("estimated_margin"), s(rs, "reason")
        );
    }

    private String s(ResultSet resultSet, String column) throws SQLException {
        return resultSet.getString(column);
    }

    private Instant ts(Timestamp timestamp) {
        return timestamp == null ? Instant.EPOCH : timestamp.toInstant();
    }
}
