package com.example.crossborder.service;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.crossborder.config.ReportProperties;
import com.example.crossborder.model.AdminSettings;
import com.example.crossborder.model.DomesticLink;
import com.example.crossborder.model.TrendCandidate;
import com.example.crossborder.model.TrendReport;
import com.example.crossborder.repository.TrendRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

class TrendReportServiceTest {
    @Test
    void japanCollectionKeepsOriginalChineseTitleSummaryAndSourcingFlow() {
        TrendRepository repository = mock(TrendRepository.class);
        DemoJapanTrendSource demo = mock(DemoJapanTrendSource.class);
        DomesticSearchService domestic = mock(DomesticSearchService.class);
        AdminSettingsService settingsService = mock(AdminSettingsService.class);
        ExternalTrendDataSource external = mock(ExternalTrendDataSource.class);
        ExchangeRateService exchangeRates = mock(ExchangeRateService.class);
        PlatformTransactionManager transactions = mock(PlatformTransactionManager.class);
        LocalDate date = LocalDate.of(2026, 7, 31);
        AdminSettings settings = new AdminSettings(
            List.of("demo"), List.of("1688"), List.of("玩具"), List.of("日本"), "demo",
            "0 30 8 * * *", 200, new BigDecimal("0.048"), false, new BigDecimal("18"), false
        );
        TrendCandidate candidate = new TrendCandidate(
            "玩具", "おもちゃ収納", "儿童玩具收纳盒", "儿童 玩具 收纳盒", "TikTok JP / demo",
            "https://example.com", null, 80, 10, 20, 70, new BigDecimal("1000"), "JPY", "原有中文理由"
        );
        DomesticLink link = new DomesticLink(
            0, 0, "1688", "儿童玩具收纳盒 - 1688搜索", "https://example.com/supplier",
            new BigDecimal("20"), "中文采购说明"
        );
        TrendReport expected = new TrendReport(1, date, "demo", "日报", "摘要", Instant.now(), List.of());
        when(settingsService.get()).thenReturn(settings);
        when(demo.fetch(date)).thenReturn(List.of(candidate));
        when(exchangeRates.resolveToCny("JPY", settings.jpyCnyRate(), false)).thenReturn(new BigDecimal("0.05"));
        when(domestic.search(candidate, new BigDecimal("50.00"), settings.supplierSites(), "jp")).thenReturn(List.of(link));
        when(transactions.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        when(repository.createReport(any(), any(), any(), any(), any())).thenReturn(1L);
        when(repository.byId(1L)).thenReturn(Optional.of(expected));
        ArgumentCaptor<String> title = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> summary = ArgumentCaptor.forClass(String.class);

        assertSame(expected, new TrendReportService(
            demo, domestic, repository, settingsService, external,
            new ReportProperties("0 30 8 * * *", "Asia/Shanghai", new BigDecimal("0.048"), new BigDecimal("18"), "demo", 200, new BigDecimal("0.12"), new BigDecimal("0.03"), BigDecimal.ZERO, BigDecimal.ZERO),
            new ProfitCalculator(), exchangeRates, transactions
        ).collect(date, "jp", true));

        verify(repository).createReport(eq(date), eq("jp:demo"), eq("TikTok JP / demo"), title.capture(), summary.capture());
        assertEquals("日本市场跨境热品日报 2026-07-31", title.getValue());
        assertEquals(
            "本次采集 1 个商品，其中真实目录 0 个、演示 1 个；来源=TikTok JP / demo；按销量指数筛选，按综合热度倒序；币种=JPY。",
            summary.getValue()
        );
        verify(domestic).search(candidate, new BigDecimal("50.00"), settings.supplierSites(), "jp");
    }

    @Test
    void existingReportUsesStableSourceKeyInsteadOfEditableDisplayName() {
        TrendRepository repository = mock(TrendRepository.class);
        DemoJapanTrendSource demo = mock(DemoJapanTrendSource.class);
        DomesticSearchService domestic = mock(DomesticSearchService.class);
        AdminSettingsService settings = mock(AdminSettingsService.class);
        ExternalTrendDataSource external = mock(ExternalTrendDataSource.class);
        ExchangeRateService exchangeRates = mock(ExchangeRateService.class);
        PlatformTransactionManager transactions = mock(PlatformTransactionManager.class);
        TrendReport expected = new TrendReport(9, LocalDate.of(2026, 7, 16), "任意可编辑名称", "日报", "", Instant.now(), List.of());
        when(repository.byDateAndSourceKey(eq(LocalDate.of(2026, 7, 16)), eq("jp:demo"))).thenReturn(Optional.of(expected));
        when(settings.get()).thenReturn(new com.example.crossborder.model.AdminSettings(
            List.of("demo"), List.of("1688"), List.of("玩具"), List.of("日本"), "demo",
            "0 30 8 * * *", 20, new BigDecimal("0.048"), false, new BigDecimal("18"), false
        ));

        TrendReportService service = new TrendReportService(
            demo, domestic, repository, settings, external,
            new ReportProperties("0 30 8 * * *", "Asia/Shanghai", new BigDecimal("0.048"), new BigDecimal("18"), "demo", 20, new BigDecimal("0.12"), new BigDecimal("0.03"), BigDecimal.ZERO, BigDecimal.ZERO),
            new ProfitCalculator(), exchangeRates, transactions
        );

        assertSame(expected, service.collect(LocalDate.of(2026, 7, 16), false));
        verifyNoInteractions(demo, domestic, external, exchangeRates);
    }

    @Test
    void nonJapanMarketsUseTheirOwnExternalReportKey() {
        TrendRepository repository = mock(TrendRepository.class);
        DemoJapanTrendSource demo = mock(DemoJapanTrendSource.class);
        DomesticSearchService domestic = mock(DomesticSearchService.class);
        AdminSettingsService settings = mock(AdminSettingsService.class);
        ExternalTrendDataSource external = mock(ExternalTrendDataSource.class);
        ExchangeRateService exchangeRates = mock(ExchangeRateService.class);
        PlatformTransactionManager transactions = mock(PlatformTransactionManager.class);
        LocalDate date = LocalDate.of(2026, 7, 17);
        TrendReport expected = new TrendReport(10, date, "WooCommerce US", "US report", "", Instant.now(), List.of());
        when(repository.byDateAndSourceKey(date, "us:external")).thenReturn(Optional.of(expected));
        when(settings.get()).thenReturn(new com.example.crossborder.model.AdminSettings(
            List.of("demo"), List.of("1688"), List.of("Toys"), List.of("United States"), "demo",
            "0 30 8 * * *", 200, new BigDecimal("0.048"), false, new BigDecimal("18"), false
        ));

        TrendReportService service = new TrendReportService(
            demo, domestic, repository, settings, external,
            new ReportProperties("0 30 8 * * *", "Asia/Shanghai", new BigDecimal("0.048"), new BigDecimal("18"), "demo", 200, new BigDecimal("0.12"), new BigDecimal("0.03"), BigDecimal.ZERO, BigDecimal.ZERO),
            new ProfitCalculator(), exchangeRates, transactions
        );

        assertSame(expected, service.collect(date, "us", false));
        verifyNoInteractions(demo, domestic, external, exchangeRates);
    }
}
