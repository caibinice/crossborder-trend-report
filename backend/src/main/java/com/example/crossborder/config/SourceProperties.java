package com.example.crossborder.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.sources")
public record SourceProperties(
    boolean googleTrendsEnabled,
    String googleTrendsRegions,
    boolean frankfurterEnabled,
    boolean woocommerceEnabled,
    String woocommerceStoreUrls,
    String tiktokMode,
    String apifyToken,
    String apifyTikTokActor,
    String tiktokResearchToken,
    String tiktokShopApiKey,
    String amazonMode,
    String rainforestApiKey,
    String keepaApiKey,
    String serpapiKey,
    String rakutenApplicationId,
    String rakutenAccessKey,
    String rakutenAffiliateId,
    String rakutenApiVersion,
    String yahooShoppingClientId,
    String supplierMode,
    String outboundProxy
) {}
