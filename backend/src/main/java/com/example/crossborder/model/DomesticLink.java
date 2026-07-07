package com.example.crossborder.model;
import java.math.BigDecimal;
public record DomesticLink(long id,long productId,String platform,String title,String url,BigDecimal priceCny,String note) {}
