package com.example.crossborder.service;

import com.example.crossborder.model.AdminSettings;
import com.example.crossborder.model.TrendCandidate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

@Service
public class WooCommerceTrendSource {
    private final ExternalDataSourceService external;
    private final ObjectMapper json;

    public WooCommerceTrendSource(ExternalDataSourceService external, ObjectMapper json) {
        this.external = external;
        this.json = json;
    }

    public List<TrendCandidate> fetch(AdminSettings settings) {
        return fetch(settings, "jp");
    }

    public List<TrendCandidate> fetch(AdminSettings settings, String marketKey) {
        MarketCatalog.Market market = MarketCatalog.get(marketKey);
        List<String> stores = external.woocommerceStores(market.key());
        if (stores.isEmpty()) return List.of();
        int perStore = Math.min(Math.max(settings.maxCategories() * settings.productsPerCategory(), 10), 100);
        List<TrendCandidate> candidates = new ArrayList<>();
        DataSourceAccessException lastFailure = null;
        for (String store : stores) {
            try {
                String body = external.get(external.woocommerceProductsUrl(store, perStore));
                candidates.addAll(parse(body, store, market.key()));
            } catch (DataSourceAccessException exception) {
                lastFailure = exception;
            }
        }
        if (candidates.isEmpty() && lastFailure != null) throw lastFailure;
        return candidates;
    }

    List<TrendCandidate> parse(String body, String storeUrl) {
        return parse(body, storeUrl, "jp");
    }

    List<TrendCandidate> parse(String body, String storeUrl, String marketKey) {
        try {
            MarketCatalog.Market market = MarketCatalog.get(marketKey);
            JsonNode root = json.readTree(body);
            if (!root.isArray()) throw new DataSourceAccessException("WooCommerce 返回格式不是商品数组");
            String host = URI.create(storeUrl).getHost();
            List<TrendCandidate> candidates = new ArrayList<>();
            int position = 0;
            for (JsonNode product : root) {
                position++;
                String title = clean(product.path("name").asText(""));
                if (title.isBlank()) continue;
                JsonNode prices = product.path("prices");
                int minorUnit = Math.max(0, Math.min(prices.path("currency_minor_unit").asInt(2), 8));
                BigDecimal price = decimal(prices.path("price").asText("0"), minorUnit);
                if (price.signum() <= 0) continue;
                String currency = prices.path("currency_code").asText(market.currency()).toUpperCase(Locale.ROOT);
                String sourceCategory = firstText(product.path("categories"), "name");
                String category = classify(title + " " + sourceCategory);
                double rating = product.path("average_rating").asDouble(0);
                long reviewCount = product.path("review_count").asLong(0);
                double heat = Math.min(100D, Math.max(35D,
                    101D - position * 1.8D + Math.min(reviewCount, 500) / 25D + rating * 1.5D));
                double volumeSignal = reviewCount > 0 ? Math.log1p(reviewCount) : 1D / position;
                double amountSignal = volumeSignal * price.doubleValue();
                String image = firstText(product.path("images"), "src");
                String description = clean(product.path("short_description").asText(""));
                String reason = "Public storefront popularity rank #" + position + "; source category: "
                    + value(sourceCategory, "Uncategorized")
                    + (reviewCount > 0 ? "; reviews=" + reviewCount + ", rating=" + rating : "")
                    + (description.isBlank() ? "." : "; " + abbreviate(description, 120));
                candidates.add(new TrendCandidate(
                    category, title, title, keyword(title),
                    "WooCommerce " + market.key().toUpperCase(Locale.ROOT) + " / " + host,
                    product.path("permalink").asText(storeUrl), image, round(heat), volumeSignal, amountSignal, 50D,
                    price, currency, reason
                ));
            }
            if (candidates.isEmpty()) throw new DataSourceAccessException("WooCommerce 目录没有可用在售商品");
            return candidates;
        } catch (DataSourceAccessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new DataSourceAccessException("WooCommerce 商品解析失败：" + exception.getMessage(), exception);
        }
    }

    private BigDecimal decimal(String value, int minorUnit) {
        try {
            return new BigDecimal(value).movePointLeft(minorUnit).setScale(Math.min(minorUnit, 4), RoundingMode.HALF_UP);
        } catch (NumberFormatException exception) {
            return BigDecimal.ZERO;
        }
    }

    private String firstText(JsonNode array, String field) {
        if (!array.isArray() || array.isEmpty()) return "";
        return clean(array.get(0).path(field).asText(""));
    }

    private String classify(String value) {
        String text = value.toLowerCase(Locale.ROOT);
        if (contains(text, "snack", "candy", "chocolate", "wafer", "corn", "peanut", "food", "tea", "coffee", "cookie", "ramen", "rice", "drink", "食品", "菓子")) return "Food";
        if (contains(text, "beauty", "skin", "cosmetic", "makeup", "hair", "美妆", "化粧")) return "Beauty";
        if (contains(text, "toy", "figure", "plush", "game", "玩具", "おもちゃ")) return "Toys";
        if (contains(text, "pet", "cat", "dog", "宠物", "ペット")) return "Pet Supplies";
        if (contains(text, "phone", "camera", "electronic", "digital", "watch", "数码", "スマホ")) return "Electronics";
        if (contains(text, "kitchen", "cook", "cup", "plate", "厨房", "キッチン")) return "Kitchen";
        if (contains(text, "fashion", "shirt", "dress", "wear", "jacket", "shoe", "bag", "服饰", "ファッション")) return "Fashion";
        if (contains(text, "baby", "kids", "child", "母婴", "ベビー")) return "Baby";
        if (contains(text, "outdoor", "camp", "garden", "fitness", "sport", "户外")) return "Outdoors";
        return "Home & Living";
    }

    private boolean contains(String text, String... needles) {
        for (String needle : needles) if (text.contains(needle)) return true;
        return false;
    }

    private String keyword(String title) {
        return title.replaceAll("[^\\p{L}\\p{N}]+", " ").trim();
    }

    private String clean(String value) {
        return HtmlUtils.htmlUnescape(value == null ? "" : value)
            .replaceAll("<[^>]+>", " ")
            .replaceAll("\\s+", " ")
            .trim();
    }

    private String abbreviate(String value, int max) {
        return value.length() <= max ? value : value.substring(0, max) + "…";
    }

    private String value(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private double round(double value) {
        return Math.round(value * 10D) / 10D;
    }
}
