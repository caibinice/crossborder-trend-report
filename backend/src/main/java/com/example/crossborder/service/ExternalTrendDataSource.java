package com.example.crossborder.service;

import com.example.crossborder.model.AdminSettings;
import com.example.crossborder.model.TrendCandidate;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ExternalTrendDataSource implements TrendDataSource {
    private static final Logger log = LoggerFactory.getLogger(ExternalTrendDataSource.class);

    private final WooCommerceTrendSource woocommerce;
    private final RakutenTrendSource rakuten;
    private final YahooShoppingTrendSource yahoo;
    private final RainforestTrendSource rainforest;
    private final SmartCandidateEnrichmentService enrichment;

    public ExternalTrendDataSource(
        WooCommerceTrendSource woocommerce,
        RakutenTrendSource rakuten,
        YahooShoppingTrendSource yahoo,
        RainforestTrendSource rainforest,
        SmartCandidateEnrichmentService enrichment
    ) {
        this.woocommerce = woocommerce;
        this.rakuten = rakuten;
        this.yahoo = yahoo;
        this.rainforest = rainforest;
        this.enrichment = enrichment;
    }

    @Override
    public List<TrendCandidate> fetch(LocalDate date, AdminSettings settings) {
        List<TrendCandidate> candidates = new ArrayList<>();
        addSafely(candidates, "WooCommerce", () -> woocommerce.fetch(settings));
        addSafely(candidates, "Yahoo Shopping", () -> yahoo.fetch(settings));
        addSafely(candidates, "Rakuten", () -> rakuten.fetch(settings));
        addSafely(candidates, "Rainforest", () -> rainforest.fetch(settings));
        List<TrendCandidate> unique = deduplicate(candidates);
        return enrichment.enrich(unique, settings);
    }

    public List<TrendCandidate> preview(String sourceKey, AdminSettings settings) {
        return switch (sourceKey) {
            case "woocommerce" -> woocommerce.fetch(settings);
            case "yahoo-shopping" -> yahoo.fetch(settings);
            case "rakuten" -> rakuten.preview(settings);
            case "rainforest" -> rainforest.fetch(settings);
            default -> throw new ApiValidationException("该数据源不支持商品连接测试");
        };
    }

    private void addSafely(List<TrendCandidate> target, String source, Supplier<List<TrendCandidate>> fetcher) {
        try {
            target.addAll(fetcher.get());
        } catch (RuntimeException exception) {
            log.warn("{} 数据源采集失败，继续使用其他已配置来源: {}", source, rootMessage(exception));
        }
    }

    private List<TrendCandidate> deduplicate(List<TrendCandidate> candidates) {
        LinkedHashMap<String, TrendCandidate> unique = new LinkedHashMap<>();
        for (TrendCandidate candidate : candidates) {
            String key = candidate.sourceUrl() == null || candidate.sourceUrl().isBlank()
                ? (candidate.sourcePlatform() + "|" + candidate.productNameJp()).toLowerCase(Locale.ROOT)
                : candidate.sourceUrl().trim().toLowerCase(Locale.ROOT);
            unique.putIfAbsent(key, candidate);
        }
        return List.copyOf(unique.values());
    }

    private String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) current = current.getCause();
        return current.getMessage() == null ? throwable.getClass().getSimpleName() : current.getMessage();
    }
}
