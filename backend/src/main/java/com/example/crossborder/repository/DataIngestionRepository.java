package com.example.crossborder.repository;

import com.example.crossborder.model.CollectionRun;
import com.example.crossborder.model.ExchangeRateSnapshot;
import com.example.crossborder.model.TrendSignal;
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

@Repository
public class DataIngestionRepository {
    private final JdbcTemplate jdbc;

    public DataIngestionRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public long startRun(String sourceKey, String triggerType) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO data_collection_runs(source_key,trigger_type,status,item_count,message) VALUES(?,?,'running',0,'采集中')",
                Statement.RETURN_GENERATED_KEYS
            );
            statement.setString(1, sourceKey);
            statement.setString(2, triggerType);
            return statement;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }

    public CollectionRun completeRun(long id, int itemCount, String message) {
        jdbc.update(
            "UPDATE data_collection_runs SET status='success',item_count=?,message=?,finished_at=CURRENT_TIMESTAMP WHERE id=?",
            Math.max(itemCount, 0), limit(message), id
        );
        return runById(id).orElseThrow();
    }

    public CollectionRun failRun(long id, String message) {
        jdbc.update(
            "UPDATE data_collection_runs SET status='fail',message=?,finished_at=CURRENT_TIMESTAMP WHERE id=?",
            limit(message), id
        );
        return runById(id).orElseThrow();
    }

    public Optional<CollectionRun> runById(long id) {
        return jdbc.query("SELECT * FROM data_collection_runs WHERE id=?", this::run, id).stream().findFirst();
    }

    public List<CollectionRun> recentRuns(int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 100);
        return jdbc.query("SELECT * FROM data_collection_runs ORDER BY started_at DESC,id DESC LIMIT ?", this::run, safeLimit);
    }

    public int saveSignals(List<TrendSignal> signals) {
        int saved = 0;
        for (TrendSignal signal : signals) {
            saved += jdbc.update("""
                INSERT INTO trend_signals(
                  source_key,region,keyword,traffic_label,traffic_value,source_url,image_url,published_at,fetched_at
                ) VALUES(?,?,?,?,?,?,?,?,CURRENT_TIMESTAMP)
                ON DUPLICATE KEY UPDATE
                  traffic_label=VALUES(traffic_label),traffic_value=VALUES(traffic_value),source_url=VALUES(source_url),
                  image_url=VALUES(image_url),fetched_at=CURRENT_TIMESTAMP
                """,
                signal.sourceKey(), signal.region(), signal.keyword(), signal.trafficLabel(), signal.trafficValue(),
                signal.sourceUrl(), signal.imageUrl(), Timestamp.from(signal.publishedAt())
            );
        }
        return saved;
    }

    public List<TrendSignal> signals(String region, int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 100);
        if (region == null || region.isBlank()) {
            return jdbc.query(
                "SELECT * FROM trend_signals ORDER BY published_at DESC,traffic_value DESC,id DESC LIMIT ?",
                this::signal, safeLimit
            );
        }
        return jdbc.query(
            "SELECT * FROM trend_signals WHERE region=? ORDER BY published_at DESC,traffic_value DESC,id DESC LIMIT ?",
            this::signal, region.trim().toUpperCase(), safeLimit
        );
    }

    public int countSignals() {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM trend_signals", Integer.class);
        return count == null ? 0 : count;
    }

    public ExchangeRateSnapshot saveRate(ExchangeRateSnapshot rate) {
        jdbc.update("""
            INSERT INTO exchange_rates(base_currency,quote_currency,rate_date,rate_value,provider,fetched_at)
            VALUES(?,?,?,?,?,CURRENT_TIMESTAMP)
            ON DUPLICATE KEY UPDATE rate_value=VALUES(rate_value),provider=VALUES(provider),fetched_at=CURRENT_TIMESTAMP
            """,
            rate.baseCurrency(), rate.quoteCurrency(), java.sql.Date.valueOf(rate.rateDate()), rate.rateValue(), rate.provider()
        );
        return rate(rate.baseCurrency(), rate.quoteCurrency()).orElseThrow();
    }

    public Optional<ExchangeRateSnapshot> rate(String base, String quote) {
        return jdbc.query("""
            SELECT * FROM exchange_rates WHERE base_currency=? AND quote_currency=?
            ORDER BY rate_date DESC,fetched_at DESC LIMIT 1
            """, this::rate, base, quote).stream().findFirst();
    }

    private CollectionRun run(ResultSet rs, int rowNum) throws SQLException {
        return new CollectionRun(
            rs.getLong("id"), text(rs, "source_key"), text(rs, "trigger_type"), text(rs, "status"),
            rs.getInt("item_count"), text(rs, "message"), instant(rs.getTimestamp("started_at")),
            nullableInstant(rs.getTimestamp("finished_at"))
        );
    }

    private TrendSignal signal(ResultSet rs, int rowNum) throws SQLException {
        return new TrendSignal(
            rs.getLong("id"), text(rs, "source_key"), text(rs, "region"), text(rs, "keyword"),
            text(rs, "traffic_label"), rs.getLong("traffic_value"), text(rs, "source_url"), text(rs, "image_url"),
            instant(rs.getTimestamp("published_at")), instant(rs.getTimestamp("fetched_at"))
        );
    }

    private ExchangeRateSnapshot rate(ResultSet rs, int rowNum) throws SQLException {
        LocalDate rateDate = rs.getDate("rate_date").toLocalDate();
        return new ExchangeRateSnapshot(
            rs.getLong("id"), text(rs, "base_currency"), text(rs, "quote_currency"), rateDate,
            rs.getBigDecimal("rate_value"), text(rs, "provider"), instant(rs.getTimestamp("fetched_at"))
        );
    }

    private String text(ResultSet rs, String column) throws SQLException {
        return rs.getString(column);
    }

    private Instant instant(Timestamp timestamp) {
        return timestamp == null ? Instant.EPOCH : timestamp.toInstant();
    }

    private Instant nullableInstant(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }

    private String limit(String message) {
        String value = message == null ? "" : message;
        return value.length() <= 1000 ? value : value.substring(0, 1000);
    }
}
