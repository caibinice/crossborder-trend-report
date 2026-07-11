package com.example.crossborder.service;

import com.example.crossborder.model.AdminSettings;
import com.example.crossborder.model.DomesticLink;
import com.example.crossborder.model.TrendCandidate;
import com.example.crossborder.model.TrendProduct;
import com.example.crossborder.model.TrendReport;
import com.example.crossborder.repository.TrendRepository;
import com.example.crossborder.config.ReportProperties;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class TrendReportService {
    private final DemoJapanTrendSource source;
    private final DomesticSearchService domestic;
    private final TrendRepository repo;
    private final AdminSettingsService settingsService;
    private final ExternalTrendDataSource externalSource;
    private final ReportProperties properties;
    private final ProfitCalculator profitCalculator;

    public TrendReportService(DemoJapanTrendSource source, DomesticSearchService domestic, TrendRepository repo, AdminSettingsService settingsService, ExternalTrendDataSource externalSource, ReportProperties properties, ProfitCalculator profitCalculator) {
        this.source = source;
        this.domestic = domestic;
        this.repo = repo;
        this.settingsService = settingsService;
        this.externalSource = externalSource;
        this.properties = properties;
        this.profitCalculator = profitCalculator;
    }

    public TrendReport collect(LocalDate date) { return collect(date, false); }

    public TrendReport collect(LocalDate date, boolean force) {
        AdminSettings settings = settingsService.get();
        List<String> allowedCategories = settings.categories();
        List<TrendCandidate> rawCandidates = switch (properties.sourceMode()) {
            case "external" -> externalOrDemo(date, settings);
            case "mixed" -> merge(source.fetch(date), externalSource.fetch(date, settings));
            default -> source.fetch(date);
        };
        List<TrendCandidate> candidates = rawCandidates.stream()
            .filter(c -> allowedCategories == null || allowedCategories.isEmpty() || allowedCategories.contains(c.category()))
            .sorted(Comparator.comparingDouble(TrendCandidate::heatScore).reversed())
            .limit(Math.max(1, settings.maxProducts()))
            .toList();
        String mode = String.join(" + ", settings.foreignSources());
        if (!force) {
            Optional<TrendReport> existing = repo.byDateAndMode(date, mode);
            if (existing.isPresent()) return existing.get();
        }
        String summary = "今日按后台配置筛选 " + candidates.size() + " 个日本热品候选；数据源=" + mode + "；品类=" + String.join("/", settings.categories()) + "。未配置真实 API Key 时使用 demo 趋势与国内搜索链接回退。";
        long reportId = repo.createReport(date, mode, "日本跨境热品日报 " + date, summary);
        int rank = 1;
        for (TrendCandidate c : candidates) {
            BigDecimal jpCny = c.jpPriceJpy().multiply(settings.jpyCnyRate()).setScale(2, RoundingMode.HALF_UP);
            List<DomesticLink> links = domestic.search(c, jpCny);
            BigDecimal cost = links.stream().map(DomesticLink::priceCny).min(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
            BigDecimal platformFee = jpCny.multiply(properties.platformFeeRate());
            BigDecimal paymentFee = jpCny.multiply(properties.paymentFeeRate());
            BigDecimal tax = jpCny.multiply(properties.taxRate());
            ProfitCalculator.Result result = profitCalculator.calculate(jpCny, cost, settings.defaultShippingCny(), platformFee, paymentFee, tax, properties.defaultAdCostCny());
            BigDecimal profit = result.profitCny();
            double margin = result.margin().doubleValue();
            repo.addProduct(new TrendProduct(0, reportId, rank++, c.category(), c.productNameJp(), c.productNameCn(), c.keywords(), c.sourcePlatform(), c.sourceUrl(), c.heatScore(), c.jpPriceJpy(), jpCny, cost, settings.defaultShippingCny(), profit, margin, c.reason(), links));
        }
        return repo.byDate(date).orElseThrow();
    }

    private List<TrendCandidate> externalOrDemo(LocalDate date, AdminSettings settings) {
        List<TrendCandidate> external = externalSource.fetch(date, settings);
        return external.isEmpty() ? source.fetch(date) : external;
    }

    private List<TrendCandidate> merge(List<TrendCandidate> demo, List<TrendCandidate> external) {
        List<TrendCandidate> out = new java.util.ArrayList<>(external);
        out.addAll(demo);
        return out;
    }

    public Optional<TrendReport> latest() { return repo.latest(); }
    public Optional<TrendReport> byDate(LocalDate date) { return repo.byDate(date); }
    public List<TrendReport> list() { return repo.list(); }
    public int countReports() { return repo.countReports(); }
    public int countProducts() { return repo.countProducts(); }
    public String mode() { return String.join(" + ", settingsService.get().foreignSources()); }
    public String schedule() { return settingsService.get().frequencyCron() + " Asia/Shanghai"; }
}
