package com.example.crossborder.service;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class MarketCatalog {
    private static final Map<String, Market> MARKETS = Map.of(
        "jp", new Market("jp", "日本市场", "JP", "JPY"),
        "us", new Market("us", "美国市场", "US", "USD"),
        "sea", new Market("sea", "东南亚市场", "SG", "SGD")
    );
    private static final List<String> KEYS = List.of("jp", "us", "sea");
    private static final Map<String, String> CHINESE_CATEGORIES = Map.ofEntries(
        Map.entry("toys", "玩具"),
        Map.entry("home & living", "家居"),
        Map.entry("beauty", "美妆"),
        Map.entry("pet supplies", "宠物"),
        Map.entry("electronics", "数码"),
        Map.entry("outdoors", "户外"),
        Map.entry("baby", "母婴"),
        Map.entry("kitchen", "厨房"),
        Map.entry("fashion", "服饰"),
        Map.entry("food", "食品"),
        Map.entry("automotive", "汽车"),
        Map.entry("stationery", "文具"),
        Map.entry("health", "健康"),
        Map.entry("beading", "串珠")
    );

    private MarketCatalog() {}

    public static Market get(String marketKey) {
        String key = marketKey == null || marketKey.isBlank()
            ? "jp"
            : marketKey.trim().toLowerCase(Locale.ROOT);
        Market market = MARKETS.get(key);
        if (market == null) {
            throw new ApiValidationException("marketKey 必须是以下值之一：" + String.join("、", KEYS));
        }
        return market;
    }

    public static List<String> keys() {
        return KEYS;
    }

    public static List<String> categories(List<String> configured, String marketKey) {
        List<String> source = configured == null ? List.of() : configured;
        get(marketKey);
        LinkedHashMap<String, String> unique = new LinkedHashMap<>();
        for (String category : source) {
            if (category == null || category.isBlank()) continue;
            String normalized = category.trim();
            String chinese = chineseCategory(normalized);
            unique.putIfAbsent(chinese.toLowerCase(Locale.ROOT), chinese);
        }
        return List.copyOf(unique.values());
    }

    public static String chineseCategory(String category) {
        if (category == null || category.isBlank()) return "家居";
        String normalized = category.trim();
        return CHINESE_CATEGORIES.getOrDefault(normalized.toLowerCase(Locale.ROOT), normalized);
    }

    public record Market(String key, String name, String trendRegion, String currency) {
        public boolean english() {
            return !"jp".equals(key);
        }
    }
}
