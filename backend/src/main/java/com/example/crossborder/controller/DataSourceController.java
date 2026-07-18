package com.example.crossborder.controller;

import com.example.crossborder.model.CollectionRun;
import com.example.crossborder.model.ExchangeRateSnapshot;
import com.example.crossborder.model.SourceCollectRequest;
import com.example.crossborder.model.SourceTestResult;
import com.example.crossborder.model.TrendSignal;
import com.example.crossborder.service.AdminAuthService;
import com.example.crossborder.service.ApiValidationException;
import com.example.crossborder.service.ExchangeRateService;
import com.example.crossborder.service.GoogleTrendsService;
import com.example.crossborder.service.SourceOperationsService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api")
public class DataSourceController {
    private final AdminAuthService auth;
    private final GoogleTrendsService trends;
    private final ExchangeRateService rates;
    private final SourceOperationsService operations;

    public DataSourceController(
        AdminAuthService auth,
        GoogleTrendsService trends,
        ExchangeRateService rates,
        SourceOperationsService operations
    ) {
        this.auth = auth;
        this.trends = trends;
        this.rates = rates;
        this.operations = operations;
    }

    @GetMapping("/trend-signals")
    public List<TrendSignal> trendSignals(
        @RequestParam(defaultValue = "JP") String region,
        @RequestParam(defaultValue = "20") int limit
    ) {
        validateLimit(limit);
        return trends.list(region, limit);
    }

    @GetMapping("/exchange-rates/latest")
    public ExchangeRateSnapshot latestRate(
        @RequestParam(defaultValue = "JPY") String base,
        @RequestParam(defaultValue = "CNY") String quote
    ) {
        return rates.latest(base, quote)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "尚未同步该币种汇率"));
    }

    @GetMapping("/admin/collection-runs")
    public List<CollectionRun> collectionRuns(
        @RequestHeader(value = "Authorization", required = false) String authorization,
        @RequestParam(defaultValue = "30") int limit
    ) {
        requireAuth(authorization);
        validateLimit(limit);
        return operations.recentRuns(limit);
    }

    @PostMapping("/admin/data-sources/{sourceKey}/test")
    public SourceTestResult test(
        @RequestHeader(value = "Authorization", required = false) String authorization,
        @PathVariable String sourceKey,
        @RequestBody(required = false) SourceCollectRequest request
    ) {
        requireAuth(authorization);
        return operations.test(sourceKey, region(request));
    }

    @PostMapping("/admin/data-sources/{sourceKey}/collect")
    public CollectionRun collect(
        @RequestHeader(value = "Authorization", required = false) String authorization,
        @PathVariable String sourceKey,
        @RequestBody(required = false) SourceCollectRequest request
    ) {
        requireAuth(authorization);
        return operations.collect(sourceKey, region(request), "manual");
    }

    private String region(SourceCollectRequest request) {
        return request == null || request.region() == null || request.region().isBlank() ? "JP" : request.region();
    }

    private void validateLimit(int limit) {
        if (limit < 1 || limit > 100) throw new ApiValidationException("limit 必须在 1 到 100 之间");
    }

    private void requireAuth(String authorization) {
        if (!auth.authorized(authorization)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "请先登录后台");
        }
    }
}
