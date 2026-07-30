package com.example.crossborder.service;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class MarketCatalog {
    private static final Map<String, Market> MARKETS = Map.of(
        "jp", new Market("jp", "Japan", "JP", "JPY"),
        "us", new Market("us", "United States", "US", "USD"),
        "sea", new Market("sea", "Southeast Asia", "SG", "SGD")
    );
    private static final List<String> KEYS = List.of("jp", "us", "sea");
    private static final Map<String, String> ENGLISH_CATEGORIES = Map.ofEntries(
        Map.entry("玩具", "Toys"),
        Map.entry("家居", "Home & Living"),
        Map.entry("美妆", "Beauty"),
        Map.entry("宠物", "Pet Supplies"),
        Map.entry("数码", "Electronics"),
        Map.entry("户外", "Outdoors"),
        Map.entry("母婴", "Baby"),
        Map.entry("厨房", "Kitchen"),
        Map.entry("服饰", "Fashion"),
        Map.entry("食品", "Food"),
        Map.entry("汽车", "Automotive"),
        Map.entry("文具", "Stationery"),
        Map.entry("健康", "Health"),
        Map.entry("串珠", "Beading")
    );

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

    public static List<String> categories(List<String> configured, String marketKey) {
        List<String> source = configured == null ? List.of() : configured;
        if ("jp".equals(get(marketKey).key())) return List.copyOf(source);
        LinkedHashMap<String, String> unique = new LinkedHashMap<>();
        for (String category : source) {
            if (category == null || category.isBlank()) continue;
            String normalized = category.trim();
            String english = ENGLISH_CATEGORIES.getOrDefault(normalized, normalized);
            unique.putIfAbsent(english.toLowerCase(Locale.ROOT), english);
        }
        return List.copyOf(unique.values());
    }

    public record Market(String key, String name, String trendRegion, String currency) {
        public boolean english() {
            return !"jp".equals(key);
        }
    }
}
