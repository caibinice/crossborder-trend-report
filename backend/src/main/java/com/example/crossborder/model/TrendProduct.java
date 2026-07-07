package com.example.crossborder.model;
import java.math.BigDecimal;
import java.util.List;
public record TrendProduct(long id,long reportId,int rank,String category,String productNameJp,String productNameCn,String keywords,String sourcePlatform,String sourceUrl,double heatScore,BigDecimal jpPriceJpy,BigDecimal jpPriceCny,BigDecimal domesticCostCny,BigDecimal shippingCny,BigDecimal estimatedProfitCny,double estimatedMargin,String reason,List<DomesticLink> domesticLinks) {}
