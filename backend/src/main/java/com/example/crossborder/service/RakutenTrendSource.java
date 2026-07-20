package com.example.crossborder.service;

import com.example.crossborder.model.AdminSettings;
import com.example.crossborder.model.TrendCandidate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class RakutenTrendSource {
    private static final int MAX_ATTEMPTS = 3;
    private static final long MIN_REQUEST_INTERVAL_MILLIS = 1_250L;
    private static final long INITIAL_RETRY_DELAY_MILLIS = 2_000L;

    private final ExternalDataSourceService external;
    private final ObjectMapper json;
    private final Object rateLimitLock = new Object();
    private long nextRequestAtMillis;

    public RakutenTrendSource(ExternalDataSourceService external, ObjectMapper json) {
        this.external = external;
        this.json = json;
    }

    public List<TrendCandidate> fetch(AdminSettings settings) {
        return fetch(settings, 5, 10);
    }

    public List<TrendCandidate> preview(AdminSettings settings) {
        return fetch(settings, 1, 3);
    }

    private List<TrendCandidate> fetch(AdminSettings settings, int maxCategories, int maxHits) {
        if (!external.rakutenConfigured()) return List.of();
        List<TrendCandidate> candidates = new ArrayList<>();
        List<String> categories = settings.categories() == null || settings.categories().isEmpty()
            ? List.of("家居")
            : settings.categories();
        int categoryCount = Math.min(categories.size(), maxCategories);
        int hits = Math.max(1, Math.min(maxHits, settings.maxProducts() / categoryCount + 1));
        DataSourceAccessException lastFailure = null;
        for (String category : categories.stream().limit(categoryCount).toList()) {
            String url = external.rakutenSearchUrl(JapaneseCategoryQueries.forCategory(category), hits).orElseThrow();
            try {
                candidates.addAll(parse(fetchWithRetry(url), category));
            } catch (DataSourceAccessException exception) {
                lastFailure = exception;
                if (exception.hasStatus(429)) break;
            }
        }
        if (candidates.isEmpty() && lastFailure != null) throw lastFailure;
        return candidates;
    }

    private String fetchWithRetry(String url) {
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            awaitRequestSlot();
            try {
                return external.get(url, external.rakutenHeaders());
            } catch (DataSourceAccessException exception) {
                if (!exception.hasStatus(429) || attempt == MAX_ATTEMPTS) throw exception;
                sleep(INITIAL_RETRY_DELAY_MILLIS << (attempt - 1));
            }
        }
        throw new DataSourceAccessException("Rakuten 请求重试失败");
    }

    private void awaitRequestSlot() {
        synchronized (rateLimitLock) {
            long now = System.currentTimeMillis();
            sleep(Math.max(0L, nextRequestAtMillis - now));
            nextRequestAtMillis = System.currentTimeMillis() + MIN_REQUEST_INTERVAL_MILLIS;
        }
    }

    private void sleep(long millis) {
        if (millis <= 0) return;
        try {
            Thread.sleep(millis);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new DataSourceAccessException("Rakuten 请求等待被中断", exception);
        }
    }

    List<TrendCandidate> parse(String body, String category) {
        try {
            JsonNode root = json.readTree(body);
            JsonNode items = root.has("items") ? root.path("items") : root.path("Items");
            List<TrendCandidate> candidates = new ArrayList<>();
            int position = 0;
            for (JsonNode wrapper : items) {
                position++;
                JsonNode item = wrapper.has("Item") ? wrapper.path("Item") : wrapper;
                String title = item.path("itemName").asText("").trim();
                BigDecimal price = item.path("itemPrice").decimalValue();
                if (title.isBlank() || price.signum() <= 0) continue;
                long reviews = item.path("reviewCount").asLong(0);
                double rating = item.path("reviewAverage").asDouble(0);
                double heat = Math.min(100, 95 - position + Math.log10(reviews + 1) * 2 + rating);
                String shopName = item.path("shopName").asText("").trim();
                String itemUrl = item.path("affiliateUrl").asText(item.path("itemUrl").asText("https://www.rakuten.co.jp/"));
                String evidence = "乐天评论热度排序第 " + position + "；评论=" + reviews + "，评分=" + rating
                    + (shopName.isBlank() ? "" : "；店铺=" + shopName)
                    + (item.path("shipOverseasFlag").asInt(0) == 1 ? "；支持海外配送" : "") + "。";
                candidates.add(new TrendCandidate(
                    category, title, title, JapaneseCategoryQueries.forCategory(category), "Rakuten Ichiba",
                    itemUrl, image(item), round(heat), price, "JPY", evidence
                ));
            }
            return candidates;
        } catch (Exception exception) {
            throw new DataSourceAccessException("Rakuten 商品解析失败：" + exception.getMessage(), exception);
        }
    }

    private String image(JsonNode item) {
        JsonNode images = item.has("mediumImageUrls") ? item.path("mediumImageUrls") : item.path("mediumImageUrl");
        if (!images.isArray() || images.isEmpty()) return null;
        JsonNode first = images.get(0);
        return first.isTextual() ? first.asText() : first.path("imageUrl").asText(null);
    }

    private double round(double value) { return Math.round(value * 10D) / 10D; }
}
