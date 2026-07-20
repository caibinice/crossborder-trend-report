package com.example.crossborder.model;

import java.math.BigDecimal;

public record TrendCandidate(
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
    String reason
) {
    public String sourceTitle() {
        return productNameJp;
    }
}
