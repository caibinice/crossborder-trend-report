package com.example.crossborder.service;

import java.time.LocalDate;
import java.time.ZoneId;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.support.CronTrigger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Configuration
public class DynamicReportScheduler implements SchedulingConfigurer {
    private static final Logger log = LoggerFactory.getLogger(DynamicReportScheduler.class);
    private final TrendReportService reports;
    private final AdminSettingsService settings;
    private final SourceOperationsService sources;
    public DynamicReportScheduler(TrendReportService reports, AdminSettingsService settings, SourceOperationsService sources) {
        this.reports = reports;
        this.settings = settings;
        this.sources = sources;
    }
    @Override
    public void configureTasks(ScheduledTaskRegistrar registrar) {
        registrar.addTriggerTask(
            () -> {
                sources.collectScheduledSignals();
                for (String marketKey : MarketCatalog.keys()) {
                    try {
                        reports.collect(LocalDate.now(), marketKey, false);
                    } catch (RuntimeException exception) {
                        log.warn("Scheduled report collection failed for market {}: {}", marketKey, exception.getMessage());
                    }
                }
            },
            triggerContext -> new CronTrigger(settings.get().frequencyCron(), ZoneId.of("Asia/Shanghai")).nextExecution(triggerContext)
        );
    }
}
