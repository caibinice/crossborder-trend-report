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
        List<String> stores = external.woocommerceStores();
        if (stores.isEmpty()) return List.of();
        int perStore = Math.min(Math.max(settings.maxProducts(), 10), 50);
        List<TrendCandidate> candidates = new ArrayList<>();
        for (String store : stores) {
            String body = external.get(external.woocommerceProductsUrl(store, perStore));
            candidates.addAll(parse(body, store));
        }
        return candidates;
    }

    List<TrendCandidate> parse(String body, String storeUrl) {
        try {
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
                String currency = prices.path("currency_code").asText("JPY").toUpperCase(Locale.ROOT);
                String sourceCategory = firstText(product.path("categories"), "name");
                String category = classify(title + " " + sourceCategory);
                double rating = product.path("average_rating").asDouble(0);
                long reviewCount = product.path("review_count").asLong(0);
                double heat = Math.min(100D, Math.max(35D,
                    101D - position * 1.8D + Math.min(reviewCount, 500) / 25D + rating * 1.5D));
                String image = firstText(product.path("images"), "src");
                String description = clean(product.path("short_description").asText(""));
                String reason = "公开店铺热销排序第 " + position + "；来源类目=" + value(sourceCategory, "未分类")
                    + (reviewCount > 0 ? "；评论=" + reviewCount + "，评分=" + rating : "")
                    + (description.isBlank() ? "。" : "；" + abbreviate(description, 70));
                candidates.add(new TrendCandidate(
                    category, title, title, keyword(title), "WooCommerce / " + host,
                    product.path("permalink").asText(storeUrl), image, round(heat), price, currency, reason
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
        if (contains(text, "snack", "candy", "chocolate", "wafer", "corn", "peanut", "food", "tea", "coffee", "cookie", "ramen", "rice", "drink", "食品", "菓子")) return "食品";
        if (contains(text, "beauty", "skin", "cosmetic", "makeup", "hair", "美妆", "化粧")) return "美妆";
        if (contains(text, "toy", "figure", "plush", "game", "玩具", "おもちゃ")) return "玩具";
        if (contains(text, "pet", "cat", "dog", "宠物", "ペット")) return "宠物";
        if (contains(text, "phone", "camera", "electronic", "digital", "数码", "スマホ")) return "数码";
        if (contains(text, "kitchen", "cook", "cup", "plate", "厨房", "キッチン")) return "厨房";
        if (contains(text, "stationery", "pen", "notebook", "文具")) return "文具";
        if (contains(text, "fashion", "shirt", "dress", "wear", "服饰", "ファッション")) return "服饰";
        if (contains(text, "baby", "kids", "child", "母婴", "ベビー")) return "母婴";
        if (contains(text, "health", "fitness", "健康")) return "健康";
        return "家居";
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
