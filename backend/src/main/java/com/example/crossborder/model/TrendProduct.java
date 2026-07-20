package com.example.crossborder.model;

import java.math.BigDecimal;
import java.util.List;

public record TrendProduct(
    long id,
    long reportId,
    int rank,
    String category,
    String productNameJp,
    String productNameCn,
    String keywords,
    String sourcePlatform,
    String sourceUrl,
    String imageUrl,
    double heatScore,
    double salesVolumeScore,
    double salesAmountScore,
    double aiScore,
    BigDecimal sourcePrice,
    String sourceCurrency,
    BigDecimal sourcePriceCny,
    BigDecimal domesticCostCny,
    BigDecimal shippingCny,
    BigDecimal estimatedProfitCny,
    double estimatedMargin,
    String reason,
    List<DomesticLink> domesticLinks
) {
    public String sourceTitle() {
        return productNameJp;
    }

    /** Compatibility fields for older clients while the database keeps its legacy JPY columns. */
    public BigDecimal jpPriceJpy() {
        return "JPY".equalsIgnoreCase(sourceCurrency) ? sourcePrice : null;
    }

    public BigDecimal jpPriceCny() {
        return sourcePriceCny;
    }
}
