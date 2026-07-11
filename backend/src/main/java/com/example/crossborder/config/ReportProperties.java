package com.example.crossborder.config;
import java.math.BigDecimal;
import org.springframework.boot.context.properties.ConfigurationProperties;
@ConfigurationProperties(prefix="app.report")
public record ReportProperties(String cron,String zone,BigDecimal exchangeRateJpyCny,BigDecimal defaultShippingCny,String sourceMode,int maxProducts,BigDecimal platformFeeRate,BigDecimal paymentFeeRate,BigDecimal taxRate,BigDecimal defaultAdCostCny) {}
