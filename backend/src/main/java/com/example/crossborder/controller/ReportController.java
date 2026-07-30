package com.example.crossborder.controller;

import com.example.crossborder.model.DataSourceStatus;
import com.example.crossborder.model.HealthResponse;
import com.example.crossborder.model.RunCollectRequest;
import com.example.crossborder.model.TrendReport;
import com.example.crossborder.model.TrendReportSummary;
import com.example.crossborder.service.ApiValidationException;
import com.example.crossborder.service.AdminAuthService;
import com.example.crossborder.service.ExternalDataSourceService;
import com.example.crossborder.service.TrendReportService;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
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
public class ReportController {
    private final TrendReportService reports;
    private final ExternalDataSourceService sources;
    private final AdminAuthService auth;

    public ReportController(
        TrendReportService reports,
        ExternalDataSourceService sources,
        AdminAuthService auth
    ) {
        this.reports = reports;
        this.sources = sources;
        this.auth = auth;
    }

    @GetMapping("/health")
    public HealthResponse health() {
        return new HealthResponse("ok", reports.mode(), reports.schedule(), reports.countReports(), reports.countProducts());
    }

    @GetMapping("/datasources")
    public List<DataSourceStatus> datasources(@RequestParam(defaultValue = "jp") String marketKey) {
        return sources.statuses(marketKey);
    }

    /** Kept for compatibility. New screens use the lightweight summaries endpoint. */
    @GetMapping("/reports")
    public List<TrendReport> reports(@RequestParam(required = false) String marketKey) {
        return marketKey == null || marketKey.isBlank() ? reports.list() : reports.list(marketKey);
    }

    @GetMapping("/reports/summaries")
    public List<TrendReportSummary> reportSummaries(
        @RequestParam(defaultValue = "30") int limit,
        @RequestParam(required = false) String marketKey
    ) {
        if (limit < 1 || limit > 100) {
            throw new ApiValidationException("limit 必须在 1 到 100 之间");
        }
        return marketKey == null || marketKey.isBlank()
            ? reports.listSummaries(limit)
            : reports.listSummaries(limit, marketKey);
    }

    @GetMapping("/reports/latest")
    public TrendReport latest(@RequestParam(defaultValue = "jp") String marketKey) {
        return reports.latest(marketKey).orElseThrow(() -> new ResponseStatusException(
            HttpStatus.NOT_FOUND,
            "jp".equalsIgnoreCase(marketKey) ? "暂无日报，请先手动生成" : "No report is available yet"
        ));
    }

    @GetMapping("/reports/{id}")
    public TrendReport byId(@PathVariable long id) {
        if (id <= 0) {
            throw new ApiValidationException("日报 ID 不合法");
        }
        return reports.byId(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "日报不存在"));
    }

    @GetMapping("/report")
    public TrendReport byDate(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
        @RequestParam(defaultValue = "jp") String marketKey
    ) {
        return reports.byDate(date, marketKey)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "jp".equalsIgnoreCase(marketKey) ? "该日期暂无日报，请手动生成" : "No report is available for that market and date"
            ));
    }

    @PostMapping("/collect/run")
    public TrendReport run(
        @RequestHeader(value = "Authorization", required = false) String authorization,
        @RequestBody(required = false) RunCollectRequest request
    ) {
        if (!auth.authorized(authorization)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "此操作需要先验证操作密码");
        }
        LocalDate date = request == null || request.reportDate() == null ? LocalDate.now() : request.reportDate();
        if (date.isAfter(LocalDate.now().plusDays(1))) {
            throw new ApiValidationException("不能生成超过明天的日报");
        }
        String marketKey = request == null ? "jp" : request.marketKey();
        return reports.collect(date, marketKey, request != null && Boolean.TRUE.equals(request.force()));
    }
}
