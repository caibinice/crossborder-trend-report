package com.example.crossborder.controller;

import com.example.crossborder.model.DataSourceStatus;
import com.example.crossborder.model.HealthResponse;
import com.example.crossborder.model.RunCollectRequest;
import com.example.crossborder.model.TrendReport;
import com.example.crossborder.model.TrendReportSummary;
import com.example.crossborder.service.ApiValidationException;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api")
public class ReportController {
    private final TrendReportService reports;
    private final ExternalDataSourceService sources;

    public ReportController(TrendReportService reports, ExternalDataSourceService sources) {
        this.reports = reports;
        this.sources = sources;
    }

    @GetMapping("/health")
    public HealthResponse health() {
        return new HealthResponse("ok", reports.mode(), reports.schedule(), reports.countReports(), reports.countProducts());
    }

    @GetMapping("/datasources")
    public List<DataSourceStatus> datasources() {
        return sources.statuses();
    }

    /** Kept for compatibility. New screens use the lightweight summaries endpoint. */
    @GetMapping("/reports")
    public List<TrendReport> reports() {
        return reports.list();
    }

    @GetMapping("/reports/summaries")
    public List<TrendReportSummary> reportSummaries(@RequestParam(defaultValue = "30") int limit) {
        if (limit < 1 || limit > 100) {
            throw new ApiValidationException("limit 必须在 1 到 100 之间");
        }
        return reports.listSummaries(limit);
    }

    @GetMapping("/reports/latest")
    public TrendReport latest() {
        return reports.latest().orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "暂无日报，请先手动生成"));
    }

    @GetMapping("/reports/{id}")
    public TrendReport byId(@PathVariable long id) {
        if (id <= 0) {
            throw new ApiValidationException("日报 ID 不合法");
        }
        return reports.byId(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "日报不存在"));
    }

    @GetMapping("/report")
    public TrendReport byDate(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return reports.byDate(date).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "该日期暂无日报，请手动生成"));
    }

    @PostMapping("/collect/run")
    public TrendReport run(@RequestBody(required = false) RunCollectRequest request) {
        LocalDate date = request == null || request.reportDate() == null ? LocalDate.now() : request.reportDate();
        if (date.isAfter(LocalDate.now().plusDays(1))) {
            throw new ApiValidationException("不能生成超过明天的日报");
        }
        return reports.collect(date, request != null && Boolean.TRUE.equals(request.force()));
    }
}
