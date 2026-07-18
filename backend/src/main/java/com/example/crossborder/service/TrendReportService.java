package com.example.crossborder.service;

import com.example.crossborder.config.ReportProperties;
import com.example.crossborder.model.AdminSettings;
import com.example.crossborder.model.DomesticLink;
import com.example.crossborder.model.TrendCandidate;
import com.example.crossborder.model.TrendProduct;
import com.example.crossborder.model.TrendReport;
import com.example.crossborder.model.TrendReportSummary;
import com.example.crossborder.repository.TrendRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class TrendReportService {
    private final DemoJapanTrendSource demoSource;
    private final DomesticSearchService domestic;
    private final TrendRepository repository;
    private final AdminSettingsService settingsService;
    private final ExternalTrendDataSource externalSource;
    private final ReportProperties properties;
    private final ProfitCalculator profitCalculator;
    private final ExchangeRateService exchangeRates;
    private final TransactionTemplate transactions;
    private final ConcurrentHashMap<String, ReentrantLock> localLocks = new ConcurrentHashMap<>();

    public TrendReportService(
        DemoJapanTrendSource demoSource,
        DomesticSearchService domestic,
        TrendRepository repository,
        AdminSettingsService settingsService,
        ExternalTrendDataSource externalSource,
        ReportProperties properties,
        ProfitCalculator profitCalculator,
        ExchangeRateService exchangeRates,
        PlatformTransactionManager transactionManager
    ) {
        this.demoSource = demoSource;
        this.domestic = domestic;
        this.repository = repository;
        this.settingsService = settingsService;
        this.externalSource = externalSource;
        this.properties = properties;
        this.profitCalculator = profitCalculator;
        this.exchangeRates = exchangeRates;
        this.transactions = new TransactionTemplate(transactionManager);
    }

    public TrendReport collect(LocalDate date) {
        return collect(date, false);
    }

    /**
     * Collection has two locks: a lightweight local lock to avoid duplicate outbound API calls,
     * and a row lock in the database transaction for concurrent application instances.
     */
    public TrendReport collect(LocalDate date, boolean force) {
        AdminSettings settings = settingsService.get();
        String sourceMode = canonicalSourceMode(settings.sourceMode());
        String sourceKey = "jp:" + sourceMode;
        String lockKey = date + ":" + sourceKey;
        ReentrantLock localLock = localLocks.computeIfAbsent(lockKey, ignored -> new ReentrantLock());
        localLock.lock();
        try {
            if (!force) {
                Optional<TrendReport> existing = repository.byDateAndSourceKey(date, sourceKey);
                if (existing.isPresent()) return existing.get();
            }
            ReportDraft draft = prepareDraft(date, settings, sourceMode);
            TrendReport report = transactions.execute(status -> persist(date, sourceKey, draft, force));
            return Objects.requireNonNull(report, "日报保存失败");
        } finally {
            localLock.unlock();
            if (!localLock.hasQueuedThreads()) localLocks.remove(lockKey, localLock);
        }
    }

    private TrendReport persist(LocalDate date, String sourceKey, ReportDraft draft, boolean force) {
        repository.lockForCollection(date, sourceKey);
        if (!force) {
            Optional<TrendReport> existing = repository.byDateAndSourceKey(date, sourceKey);
            if (existing.isPresent()) return existing.get();
        }
        if (force) repository.deleteByDateAndSourceKey(date, sourceKey);
        long reportId = repository.createReport(date, sourceKey, draft.sourceMode(), "日本市场跨境热品日报 " + date, draft.summary());
        int rank = 1;
        for (ProductDraft product : draft.products()) {
            repository.addProduct(new TrendProduct(
                0, reportId, rank++, product.category(), product.productNameJp(), product.productNameCn(), product.keywords(),
                product.sourcePlatform(), product.sourceUrl(), product.imageUrl(), product.heatScore(), product.sourcePrice(),
                product.sourceCurrency(), product.sourcePriceCny(), product.domesticCostCny(), product.shippingCny(),
                product.estimatedProfitCny(), product.estimatedMargin(), product.reason(), product.domesticLinks()
            ));
        }
        return repository.byId(reportId).orElseThrow(() -> new IllegalStateException("日报保存后无法读取"));
    }

    private ReportDraft prepareDraft(LocalDate date, AdminSettings settings, String sourceMode) {
        List<TrendCandidate> external = sourceMode.equals("demo") ? List.of() : externalSource.fetch(date, settings);
        List<TrendCandidate> rawCandidates = switch (sourceMode) {
            case "external" -> requireExternal(external);
            case "mixed" -> merge(demoSource.fetch(date), external);
            default -> demoSource.fetch(date);
        };
        List<String> configuredCategories = settings.categories() == null ? List.of() : settings.categories();
        List<TrendCandidate> candidates = rawCandidates.stream()
            .filter(candidate -> configuredCategories.isEmpty() || configuredCategories.contains(candidate.category()))
            .sorted(Comparator.comparingDouble(TrendCandidate::heatScore).reversed())
            .limit(Math.max(1, settings.maxProducts()))
            .toList();
        if (candidates.isEmpty()) {
            throw new ApiConflictException("数据源返回了商品，但全部被后台品类配置过滤；请调整品类后重试");
        }

        Map<String, BigDecimal> currencyRates = new HashMap<>();
        List<ProductDraft> products = new ArrayList<>();
        for (TrendCandidate candidate : candidates) {
            String currency = normalizedCurrency(candidate.sourceCurrency());
            BigDecimal rate = currencyRates.computeIfAbsent(currency,
                key -> exchangeRates.resolveToCny(key, settings.jpyCnyRate(), settings.autoExchangeRate()));
            BigDecimal sourcePrice = nonNull(candidate.sourcePrice());
            BigDecimal sourceCny = sourcePrice.multiply(rate).setScale(2, RoundingMode.HALF_UP);
            List<DomesticLink> links = domestic.search(candidate, sourceCny);
            BigDecimal cost = links.stream().map(DomesticLink::priceCny).min(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
            BigDecimal platformFee = sourceCny.multiply(value(properties.platformFeeRate()));
            BigDecimal paymentFee = sourceCny.multiply(value(properties.paymentFeeRate()));
            BigDecimal tax = sourceCny.multiply(value(properties.taxRate()));
            ProfitCalculator.Result result = profitCalculator.calculate(
                sourceCny, cost, settings.defaultShippingCny(), platformFee, paymentFee, tax, value(properties.defaultAdCostCny())
            );
            products.add(new ProductDraft(
                candidate.category(), candidate.productNameJp(), candidate.productNameCn(), candidate.keywords(),
                candidate.sourcePlatform(), candidate.sourceUrl(), candidate.imageUrl(), candidate.heatScore(), sourcePrice,
                currency, sourceCny, cost, settings.defaultShippingCny(), result.profitCny(), result.margin().doubleValue(),
                candidate.reason(), links
            ));
        }
        long realCount = products.stream().filter(product -> !product.sourcePlatform().toLowerCase(Locale.ROOT).contains("demo")).count();
        String displayMode = products.stream().map(ProductDraft::sourcePlatform).distinct().sorted().reduce((a, b) -> a + " + " + b).orElse(sourceMode);
        String summary = "本次采集 " + products.size() + " 个商品，其中真实目录 " + realCount + " 个、演示 "
            + (products.size() - realCount) + " 个；来源=" + displayMode + "；币种=" + String.join("/", currencyRates.keySet()) + "。";
        return new ReportDraft(displayMode, summary, products);
    }

    private List<TrendCandidate> requireExternal(List<TrendCandidate> external) {
        if (external.isEmpty()) {
            throw new ApiConflictException("真实商品源未配置或均采集失败。请到后台数据源配置测试连接；系统不会在 external 模式下静默回退 Demo。");
        }
        return external;
    }

    private List<TrendCandidate> merge(List<TrendCandidate> demo, List<TrendCandidate> external) {
        LinkedHashMap<String, TrendCandidate> unique = new LinkedHashMap<>();
        for (TrendCandidate candidate : external) unique.putIfAbsent(candidateKey(candidate), candidate);
        for (TrendCandidate candidate : demo) unique.putIfAbsent(candidateKey(candidate), candidate);
        return List.copyOf(unique.values());
    }

    private String candidateKey(TrendCandidate candidate) {
        String url = candidate.sourceUrl();
        if (url != null && !url.isBlank()) return url.trim().toLowerCase(Locale.ROOT);
        return (candidate.category() + "|" + candidate.productNameJp()).trim().toLowerCase(Locale.ROOT);
    }

    private String canonicalSourceMode(String configured) {
        String value = configured == null || configured.isBlank() ? properties.sourceMode() : configured;
        value = value == null ? "external" : value.trim().toLowerCase(Locale.ROOT);
        return switch (value) {
            case "demo", "mixed" -> value;
            default -> "external";
        };
    }

    private String normalizedCurrency(String value) {
        String currency = value == null || value.isBlank() ? "JPY" : value.trim().toUpperCase(Locale.ROOT);
        return currency.matches("[A-Z]{3}") ? currency : "JPY";
    }

    private BigDecimal nonNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private BigDecimal value(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    public Optional<TrendReport> latest() { return repository.latest(); }
    public Optional<TrendReport> byDate(LocalDate date) { return repository.byDate(date); }
    public Optional<TrendReport> byId(long id) { return repository.byId(id); }
    public List<TrendReport> list() { return repository.list(); }
    public List<TrendReportSummary> listSummaries(int limit) { return repository.listSummaries(limit); }
    public int countReports() { return repository.countReports(); }
    public int countProducts() { return repository.countProducts(); }

    public String mode() {
        AdminSettings settings = settingsService.get();
        return canonicalSourceMode(settings.sourceMode()) + " · " + String.join(" + ", settings.foreignSources());
    }

    public String schedule() { return settingsService.get().frequencyCron() + " Asia/Shanghai"; }

    private record ReportDraft(String sourceMode, String summary, List<ProductDraft> products) {}

    private record ProductDraft(
        String category, String productNameJp, String productNameCn, String keywords, String sourcePlatform,
        String sourceUrl, String imageUrl, double heatScore, BigDecimal sourcePrice, String sourceCurrency,
        BigDecimal sourcePriceCny, BigDecimal domesticCostCny, BigDecimal shippingCny,
        BigDecimal estimatedProfitCny, double estimatedMargin, String reason, List<DomesticLink> domesticLinks
    ) {}
}
