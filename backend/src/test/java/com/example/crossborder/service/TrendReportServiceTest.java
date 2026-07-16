package com.example.crossborder.service;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.example.crossborder.config.ReportProperties;
import com.example.crossborder.model.TrendReport;
import com.example.crossborder.repository.TrendRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;

class TrendReportServiceTest {
    @Test
    void existingReportUsesStableSourceKeyInsteadOfEditableDisplayName() {
        TrendRepository repository = mock(TrendRepository.class);
        DemoJapanTrendSource demo = mock(DemoJapanTrendSource.class);
        DomesticSearchService domestic = mock(DomesticSearchService.class);
        AdminSettingsService settings = mock(AdminSettingsService.class);
        ExternalTrendDataSource external = mock(ExternalTrendDataSource.class);
        PlatformTransactionManager transactions = mock(PlatformTransactionManager.class);
        TrendReport expected = new TrendReport(9, LocalDate.of(2026, 7, 16), "任意可编辑名称", "日报", "", Instant.now(), List.of());
        when(repository.byDateAndSourceKey(eq(LocalDate.of(2026, 7, 16)), eq("jp:demo"))).thenReturn(Optional.of(expected));

        TrendReportService service = new TrendReportService(
            demo, domestic, repository, settings, external,
            new ReportProperties("0 30 8 * * *", "Asia/Shanghai", new BigDecimal("0.048"), new BigDecimal("18"), "demo", 20, new BigDecimal("0.12"), new BigDecimal("0.03"), BigDecimal.ZERO, BigDecimal.ZERO),
            new ProfitCalculator(), transactions
        );

        assertSame(expected, service.collect(LocalDate.of(2026, 7, 16), false));
        verifyNoInteractions(demo, domestic, settings, external);
    }
}
