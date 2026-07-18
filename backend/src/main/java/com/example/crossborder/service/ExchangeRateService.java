package com.example.crossborder.service;

import com.example.crossborder.model.CollectionRun;
import com.example.crossborder.model.ExchangeRateSnapshot;
import com.example.crossborder.model.SourceTestResult;
import com.example.crossborder.repository.DataIngestionRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class ExchangeRateService {
    public static final String SOURCE_KEY = "frankfurter";

    private final ExternalDataSourceService external;
    private final DataIngestionRepository repository;
    private final ObjectMapper json;

    public ExchangeRateService(ExternalDataSourceService external, DataIngestionRepository repository, ObjectMapper json) {
        this.external = external;
        this.repository = repository;
        this.json = json;
    }

    public CollectionRun sync(String base, String quote, String triggerType) {
        String normalizedBase = currency(base);
        String normalizedQuote = currency(quote);
        if (!external.frankfurterEnabled()) {
            throw new ApiValidationException("Frankfurter 汇率源已关闭");
        }
        long runId = repository.startRun(SOURCE_KEY, triggerType);
        try {
            ExchangeRateSnapshot saved = repository.saveRate(fetch(normalizedBase, normalizedQuote));
            return repository.completeRun(
                runId, 1, saved.baseCurrency() + "/" + saved.quoteCurrency() + " = " + saved.rateValue() + "（" + saved.rateDate() + "）"
            );
        } catch (RuntimeException exception) {
            repository.failRun(runId, rootMessage(exception));
            throw exception;
        }
    }

    public SourceTestResult test(String base, String quote) {
        try {
            ExchangeRateSnapshot rate = fetch(currency(base), currency(quote));
            return new SourceTestResult(SOURCE_KEY, true, 1,
                rate.baseCurrency() + "/" + rate.quoteCurrency() + " = " + rate.rateValue(), Instant.now());
        } catch (RuntimeException exception) {
            return new SourceTestResult(SOURCE_KEY, false, 0, rootMessage(exception), Instant.now());
        }
    }

    public BigDecimal resolveToCny(String sourceCurrency, BigDecimal jpyFallback, boolean autoExchangeRate) {
        String base = currency(sourceCurrency);
        if ("CNY".equals(base)) return BigDecimal.ONE;
        if (!autoExchangeRate && "JPY".equals(base)) return positive(jpyFallback, "后台日元汇率未配置");

        Optional<ExchangeRateSnapshot> cached = latest(base, "CNY");
        if (cached.isPresent() && !cached.get().rateDate().isBefore(LocalDate.now().minusDays(4))) {
            return cached.get().rateValue();
        }
        try {
            ExchangeRateSnapshot saved = repository.saveRate(fetch(base, "CNY"));
            return saved.rateValue();
        } catch (RuntimeException exception) {
            if ("JPY".equals(base) && jpyFallback != null && jpyFallback.signum() > 0) {
                return jpyFallback;
            }
            throw new ApiConflictException(base + "/CNY 汇率同步失败，无法计算多币种利润：" + rootMessage(exception));
        }
    }

    public Optional<ExchangeRateSnapshot> latest(String base, String quote) {
        return repository.rate(currency(base), currency(quote));
    }

    ExchangeRateSnapshot fetch(String base, String quote) {
        try {
            String body = external.get(external.frankfurterRateUrl(base, quote));
            JsonNode root = json.readTree(body);
            BigDecimal rate = root.path("rate").decimalValue();
            if (rate.signum() <= 0) throw new DataSourceAccessException("汇率值不合法");
            LocalDate date = LocalDate.parse(root.path("date").asText());
            return new ExchangeRateSnapshot(0, base, quote, date, rate, "Frankfurter", Instant.now());
        } catch (DataSourceAccessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new DataSourceAccessException("Frankfurter 响应解析失败：" + exception.getMessage(), exception);
        }
    }

    private String currency(String value) {
        String normalized = value == null || value.isBlank() ? "JPY" : value.trim().toUpperCase(Locale.ROOT);
        if (!normalized.matches("[A-Z]{3}")) throw new ApiValidationException("币种必须是三位 ISO 代码");
        return normalized;
    }

    private BigDecimal positive(BigDecimal value, String message) {
        if (value == null || value.signum() <= 0) throw new ApiConflictException(message);
        return value;
    }

    private String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) current = current.getCause();
        return current.getMessage() == null ? throwable.getClass().getSimpleName() : current.getMessage();
    }
}
