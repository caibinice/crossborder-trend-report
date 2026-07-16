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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class TrendReportService {
    private final DemoJapanTrendSource source;
    private final DomesticSearchService domestic;
    private final TrendRepository repository;
    private final AdminSettingsService settingsService;
    private final ExternalTrendDataSource externalSource;
    private final ReportProperties properties;
    private final ProfitCalculator profitCalculator;
    private final TransactionTemplate transactions;
    private final ConcurrentHashMap<String, ReentrantLock> localLocks = new ConcurrentHashMap<>();

    public TrendReportService(
        DemoJapanTrendSource source,
        DomesticSearchService domestic,
        TrendRepository repository,
        AdminSettingsService settingsService,
        ExternalTrendDataSource externalSource,
        ReportProperties properties,
        ProfitCalculator profitCalculator,
        PlatformTransactionManager transactionManager
    ) {
        this.source = source;
        this.domestic = domestic;
        this.repository = repository;
        this.settingsService = settingsService;
        this.externalSource = externalSource;
        this.properties = properties;
        this.profitCalculator = profitCalculator;
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
        String sourceKey = sourceKey();
        String lockKey = date + ":" + sourceKey;
        ReentrantLock localLock = localLocks.computeIfAbsent(lockKey, ignored -> new ReentrantLock());
        localLock.lock();
        try {
            if (!force) {
                Optional<TrendReport> existing = repository.byDateAndSourceKey(date, sourceKey);
                if (existing.isPresent()) {
                    return existing.get();
                }
            }

            ReportDraft draft = prepareDraft(date);
            TrendReport report = transactions.execute(status -> persist(date, sourceKey, draft, force));
            return Objects.requireNonNull(report, "日报保存失败");
        } finally {
            localLock.unlock();
            if (!localLock.hasQueuedThreads()) {
                localLocks.remove(lockKey, localLock);
            }
        }
    }

    private TrendReport persist(LocalDate date, String sourceKey, ReportDraft draft, boolean force) {
        repository.lockForCollection(date, sourceKey);
        if (!force) {
            Optional<TrendReport> existing = repository.byDateAndSourceKey(date, sourceKey);
            if (existing.isPresent()) {
                return existing.get();
            }
        }

        if (force) {
            repository.deleteByDateAndSourceKey(date, sourceKey);
        }
        long reportId = repository.createReport(date, sourceKey, draft.sourceMode(), "日本跨境热品日报 " + date, draft.summary());
        int rank = 1;
        for (ProductDraft product : draft.products()) {
            repository.addProduct(new TrendProduct(
                0,
                reportId,
                rank++,
                product.category(),
                product.productNameJp(),
                product.productNameCn(),
                product.keywords(),
                product.sourcePlatform(),
                product.sourceUrl(),
                product.heatScore(),
                product.jpPriceJpy(),
                product.jpPriceCny(),
                product.domesticCostCny(),
                product.shippingCny(),
                product.estimatedProfitCny(),
                product.estimatedMargin(),
                product.reason(),
                product.domesticLinks()
            ));
        }
        return repository.byId(reportId).orElseThrow(() -> new IllegalStateException("日报保存后无法读取"));
    }

    private ReportDraft prepareDraft(LocalDate date) {
        AdminSettings settings = settingsService.get();
        List<TrendCandidate> rawCandidates = switch (canonicalSourceMode()) {
            case "external" -> externalOrDemo(date, settings);
            case "mixed" -> merge(source.fetch(date), externalSource.fetch(date, settings));
            default -> source.fetch(date);
        };
        List<TrendCandidate> candidates = rawCandidates.stream()
            .filter(candidate -> settings.categories() == null || settings.categories().isEmpty() || settings.categories().contains(candidate.category()))
            .sorted(Comparator.comparingDouble(TrendCandidate::heatScore).reversed())
            .limit(Math.max(1, settings.maxProducts()))
            .toList();

        List<ProductDraft> products = new ArrayList<>();
        for (TrendCandidate candidate : candidates) {
            BigDecimal jpCny = candidate.jpPriceJpy().multiply(settings.jpyCnyRate()).setScale(2, RoundingMode.HALF_UP);
            List<DomesticLink> links = domestic.search(candidate, jpCny);
            BigDecimal cost = links.stream().map(DomesticLink::priceCny).min(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
            BigDecimal platformFee = jpCny.multiply(value(properties.platformFeeRate()));
            BigDecimal paymentFee = jpCny.multiply(value(properties.paymentFeeRate()));
            BigDecimal tax = jpCny.multiply(value(properties.taxRate()));
            ProfitCalculator.Result result = profitCalculator.calculate(
                jpCny, cost, settings.defaultShippingCny(), platformFee, paymentFee, tax, value(properties.defaultAdCostCny())
            );
            products.add(new ProductDraft(
                candidate.category(), candidate.productNameJp(), candidate.productNameCn(), candidate.keywords(),
                candidate.sourcePlatform(), candidate.sourceUrl(), candidate.heatScore(), candidate.jpPriceJpy(), jpCny,
                cost, settings.defaultShippingCny(), result.profitCny(), result.margin().doubleValue(), candidate.reason(), links
            ));
        }
        String displayMode = sourceDisplayName(settings);
        String summary = "今日按后台配置筛选 " + products.size() + " 个日本热品候选；数据源=" + displayMode + "；品类=" + String.join("/", settings.categories()) + "。";
        return new ReportDraft(displayMode, summary, products);
    }

    private List<TrendCandidate> externalOrDemo(LocalDate date, AdminSettings settings) {
        List<TrendCandidate> external = externalSource.fetch(date, settings);
        return external.isEmpty() ? source.fetch(date) : external;
    }

    private List<TrendCandidate> merge(List<TrendCandidate> demo, List<TrendCandidate> external) {
        LinkedHashMap<String, TrendCandidate> unique = new LinkedHashMap<>();
        for (TrendCandidate candidate : external) {
            unique.putIfAbsent(candidateKey(candidate), candidate);
        }
        for (TrendCandidate candidate : demo) {
            unique.putIfAbsent(candidateKey(candidate), candidate);
        }
        return List.copyOf(unique.values());
    }

    private String candidateKey(TrendCandidate candidate) {
        return (candidate.category() + "|" + candidate.productNameCn()).trim().toLowerCase(Locale.ROOT);
    }

    private String sourceKey() {
        return "jp:" + canonicalSourceMode();
    }

    private String canonicalSourceMode() {
        String configured = properties.sourceMode() == null ? "demo" : properties.sourceMode().trim().toLowerCase(Locale.ROOT);
        return switch (configured) {
            case "external", "mixed" -> configured;
            default -> "demo";
        };
    }

    private String sourceDisplayName(AdminSettings settings) {
        return settings.foreignSources() == null || settings.foreignSources().isEmpty()
            ? canonicalSourceMode()
            : String.join(" + ", settings.foreignSources());
    }

    private BigDecimal value(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    public Optional<TrendReport> latest() {
        return repository.latest();
    }

    public Optional<TrendReport> byDate(LocalDate date) {
        return repository.byDate(date);
    }

    public Optional<TrendReport> byId(long id) {
        return repository.byId(id);
    }

    public List<TrendReport> list() {
        return repository.list();
    }

    public List<TrendReportSummary> listSummaries(int limit) {
        return repository.listSummaries(limit);
    }

    public int countReports() {
        return repository.countReports();
    }

    public int countProducts() {
        return repository.countProducts();
    }

    public String mode() {
        return sourceDisplayName(settingsService.get());
    }

    public String schedule() {
        return settingsService.get().frequencyCron() + " Asia/Shanghai";
    }

    private record ReportDraft(String sourceMode, String summary, List<ProductDraft> products) {}

    private record ProductDraft(
        String category,
        String productNameJp,
        String productNameCn,
        String keywords,
        String sourcePlatform,
        String sourceUrl,
        double heatScore,
        BigDecimal jpPriceJpy,
        BigDecimal jpPriceCny,
        BigDecimal domesticCostCny,
        BigDecimal shippingCny,
        BigDecimal estimatedProfitCny,
        double estimatedMargin,
        String reason,
        List<DomesticLink> domesticLinks
    ) {}
}
