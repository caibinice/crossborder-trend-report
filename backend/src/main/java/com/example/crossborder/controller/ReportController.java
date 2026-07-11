package com.example.crossborder.controller;
import com.example.crossborder.model.*;import com.example.crossborder.service.TrendReportService;import com.example.crossborder.service.ExternalDataSourceService;import java.time.LocalDate;import java.util.List;import org.springframework.format.annotation.DateTimeFormat;import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api")
public class ReportController{
 private final TrendReportService service; private final ExternalDataSourceService sources; public ReportController(TrendReportService service,ExternalDataSourceService sources){this.service=service;this.sources=sources;}
 @GetMapping("/health") public HealthResponse health(){return new HealthResponse("ok",service.mode(),service.schedule(),service.countReports(),service.countProducts());}
 @GetMapping("/datasources") public List<DataSourceStatus> datasources(){return sources.statuses();}
 @GetMapping("/reports") public List<TrendReport> reports(){return service.list();}
 @GetMapping("/reports/latest") public TrendReport latest(){return service.latest().orElseGet(()->service.collect(LocalDate.now()));}
 @GetMapping("/report") public TrendReport report(@RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate date){return service.byDate(date).orElseGet(()->service.collect(date));}
 @PostMapping("/collect/run") public TrendReport run(@RequestBody(required=false) RunCollectRequest req){LocalDate d=req==null||req.reportDate()==null?LocalDate.now():req.reportDate();return service.collect(d, req != null && Boolean.TRUE.equals(req.force()));} }

