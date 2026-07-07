package com.example.crossborder.config;
import org.springframework.boot.context.properties.ConfigurationProperties;
@ConfigurationProperties(prefix="app.sources")
public record SourceProperties(
  String tiktokMode,
  String apifyToken,
  String apifyTikTokActor,
  String tiktokResearchToken,
  String tiktokShopApiKey,
  String amazonMode,
  String rainforestApiKey,
  String keepaApiKey,
  String serpapiKey,
  String supplierMode
) {}
