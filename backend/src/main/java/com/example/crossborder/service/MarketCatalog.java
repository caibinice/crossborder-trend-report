package com.example.crossborder.service;

import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class MarketCatalog {
    private static final Map<String, Market> MARKETS = Map.of(
        "jp", new Market("jp", "Japan", "JP", "JPY"),
        "us", new Market("us", "United States", "US", "USD"),
        "sea", new Market("sea", "Southeast Asia", "SG", "SGD")
    );
    private static final List<String> KEYS = List.of("jp", "us", "sea");

    private MarketCatalog() {}

    public static Market get(String marketKey) {
        String key = marketKey == null || marketKey.isBlank()
            ? "jp"
            : marketKey.trim().toLowerCase(Locale.ROOT);
        Market market = MARKETS.get(key);
        if (market == null) {
            throw new ApiValidationException("marketKey must be one of: " + String.join(", ", KEYS));
        }
        return market;
    }

    public static List<String> keys() {
        return KEYS;
    }

    public record Market(String key, String name, String trendRegion, String currency) {}
}
