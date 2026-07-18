package com.example.crossborder.service;

import com.example.crossborder.model.AdminSettings;
import com.example.crossborder.model.CollectionRun;
import com.example.crossborder.model.SourceTestResult;
import com.example.crossborder.model.TrendCandidate;
import com.example.crossborder.repository.DataIngestionRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class SourceOperationsService {
    private final GoogleTrendsService googleTrends;
    private final ExchangeRateService exchangeRates;
    private final ExternalTrendDataSource catalogs;
    private final ExternalDataSourceService external;
    private final SmartCandidateEnrichmentService enrichment;
    private final AdminSettingsService settings;
    private final DataIngestionRepository repository;

    public SourceOperationsService(
        GoogleTrendsService googleTrends,
        ExchangeRateService exchangeRates,
        ExternalTrendDataSource catalogs,
        ExternalDataSourceService external,
        SmartCandidateEnrichmentService enrichment,
        AdminSettingsService settings,
        DataIngestionRepository repository
    ) {
        this.googleTrends = googleTrends;
        this.exchangeRates = exchangeRates;
        this.catalogs = catalogs;
        this.external = external;
        this.enrichment = enrichment;
        this.settings = settings;
        this.repository = repository;
    }

    public SourceTestResult test(String sourceKey, String region) {
        return switch (sourceKey) {
            case GoogleTrendsService.SOURCE_KEY -> googleTrends.test(region);
            case ExchangeRateService.SOURCE_KEY -> exchangeRates.test("JPY", "CNY");
            case "woocommerce", "yahoo-shopping", "rakuten", "rainforest" -> testCatalog(sourceKey);
            case "deepseek" -> enrichment.test();
            default -> new SourceTestResult(sourceKey, false, 0, "该数据源暂不支持在线测试", Instant.now());
        };
    }

    public CollectionRun collect(String sourceKey, String region, String triggerType) {
        return switch (sourceKey) {
            case GoogleTrendsService.SOURCE_KEY -> googleTrends.collect(region, triggerType);
            case ExchangeRateService.SOURCE_KEY -> exchangeRates.sync("JPY", "CNY", triggerType);
            default -> throw new ApiValidationException("商品目录请使用“采集商品并生成日报”，该操作会写入商品池和日报");
        };
    }

    public void collectScheduledSignals() {
        for (String region : external.googleTrendRegions()) {
            try {
                googleTrends.collect(region, "schedule");
            } catch (RuntimeException ignored) {
                // The failed run is already recorded. A signal source must not block the product report.
            }
        }
        try {
            exchangeRates.sync("JPY", "CNY", "schedule");
        } catch (RuntimeException ignored) {
            // TrendReportService can still use the latest cached/manual JPY fallback.
        }
    }

    public List<CollectionRun> recentRuns(int limit) {
        return repository.recentRuns(limit);
    }

    private SourceTestResult testCatalog(String sourceKey) {
        try {
            AdminSettings current = settings.get();
            List<TrendCandidate> items = catalogs.preview(sourceKey, current);
            if (items.isEmpty()) {
                return new SourceTestResult(sourceKey, false, 0, "未配置凭证，或上游没有返回可用商品", Instant.now());
            }
            return new SourceTestResult(sourceKey, true, items.size(),
                "连接成功，样本商品：" + items.get(0).productNameJp(), Instant.now());
        } catch (RuntimeException exception) {
            return new SourceTestResult(sourceKey, false, 0, rootMessage(exception), Instant.now());
        }
    }

    private String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) current = current.getCause();
        return current.getMessage() == null ? throwable.getClass().getSimpleName() : current.getMessage();
    }
}
