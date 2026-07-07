package com.example.crossborder.model;
import java.math.BigDecimal;
public record TrendCandidate(String category,String productNameJp,String productNameCn,String keywords,String sourcePlatform,String sourceUrl,double heatScore,BigDecimal jpPriceJpy,String reason) {}
