package com.example.crossborder.service;

import java.time.LocalDate;
import java.time.ZoneId;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.support.CronTrigger;

@Configuration
public class DynamicReportScheduler implements SchedulingConfigurer {
    private final TrendReportService reports;
    private final AdminSettingsService settings;
    public DynamicReportScheduler(TrendReportService reports, AdminSettingsService settings) { this.reports = reports; this.settings = settings; }
    @Override
    public void configureTasks(ScheduledTaskRegistrar registrar) {
        registrar.addTriggerTask(
            () -> reports.collect(LocalDate.now()),
            triggerContext -> new CronTrigger(settings.get().frequencyCron(), ZoneId.of("Asia/Shanghai")).nextExecution(triggerContext)
        );
    }
}
