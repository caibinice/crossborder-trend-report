package com.example.crossborder.service;

import com.example.crossborder.model.AdminSettings;
import com.example.crossborder.model.DomesticLink;
import com.example.crossborder.model.TrendCandidate;
import com.example.crossborder.model.TrendProduct;
import com.example.crossborder.model.TrendReport;
import com.example.crossborder.repository.TrendRepository;
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

    public TrendReportService(DemoJapanTrendSource source, DomesticSearchService domestic, TrendRepository repo, AdminSettingsService settingsService) {
        this.source = source;
        this.domestic = domestic;
        this.repo = repo;
        this.settingsService = settingsService;
    }

    public TrendReport collect(LocalDate date) {
        AdminSettings settings = settingsService.get();
        List<String> allowedCategories = settings.categories();
        List<TrendCandidate> candidates = source.fetch(date).stream()
            .filter(c -> allowedCategories == null || allowedCategories.isEmpty() || allowedCategories.contains(c.category()))
            .sorted(Comparator.comparingDouble(TrendCandidate::heatScore).reversed())
            .limit(Math.max(1, settings.maxProducts()))
            .toList();
        String mode = String.join(" + ", settings.foreignSources());
        String summary = "今日按后台配置筛选 " + candidates.size() + " 个日本热品候选；数据源=" + mode + "；品类=" + String.join("/", settings.categories()) + "。未配置真实 API Key 时使用 demo 趋势与国内搜索链接回退。";
        long reportId = repo.createReport(date, mode, "日本跨境热品日报 " + date, summary);
        int rank = 1;
        for (TrendCandidate c : candidates) {
            BigDecimal jpCny = c.jpPriceJpy().multiply(settings.jpyCnyRate()).setScale(2, RoundingMode.HALF_UP);
            List<DomesticLink> links = domestic.search(c, jpCny);
            BigDecimal cost = links.stream().map(DomesticLink::priceCny).min(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
            BigDecimal profit = jpCny.subtract(cost).subtract(settings.defaultShippingCny()).setScale(2, RoundingMode.HALF_UP);
            double margin = jpCny.signum() == 0 ? 0 : profit.divide(jpCny, 4, RoundingMode.HALF_UP).doubleValue();
            repo.addProduct(new TrendProduct(0, reportId, rank++, c.category(), c.productNameJp(), c.productNameCn(), c.keywords(), c.sourcePlatform(), c.sourceUrl(), c.heatScore(), c.jpPriceJpy(), jpCny, cost, settings.defaultShippingCny(), profit, margin, c.reason(), links));
        }
        return repo.byDate(date).orElseThrow();
    }

    public Optional<TrendReport> latest() { return repo.latest(); }
    public Optional<TrendReport> byDate(LocalDate date) { return repo.byDate(date); }
    public List<TrendReport> list() { return repo.list(); }
    public int countReports() { return repo.countReports(); }
    public int countProducts() { return repo.countProducts(); }
    public String mode() { return String.join(" + ", settingsService.get().foreignSources()); }
    public String schedule() { return settingsService.get().frequencyCron() + " Asia/Shanghai"; }
}
