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
public class YahooShoppingTrendSource {
    private static final long MIN_REQUEST_INTERVAL_MILLIS = 2_100L;

    private final ExternalDataSourceService external;
    private final ObjectMapper json;
    private final Object rateLimitLock = new Object();
    private long nextRequestAtMillis;

    public YahooShoppingTrendSource(ExternalDataSourceService external, ObjectMapper json) {
        this.external = external;
        this.json = json;
    }

    public List<TrendCandidate> fetch(AdminSettings settings) {
        return fetch(settings, Math.max(1, Math.min(settings.maxCategories(), 20)),
            Math.max(1, Math.min(settings.productsPerCategory(), 100)));
    }

    public List<TrendCandidate> preview(AdminSettings settings) {
        return fetch(settings, 1, 3);
    }

    private List<TrendCandidate> fetch(AdminSettings settings, int maxCategories, int maxHits) {
        if (!external.yahooConfigured()) return List.of();
        List<TrendCandidate> candidates = new ArrayList<>();
        List<String> categories = settings.categories() == null || settings.categories().isEmpty()
            ? List.of("家居")
            : settings.categories();
        int categoryCount = Math.min(categories.size(), maxCategories);
        int hits = Math.max(1, Math.min(maxHits, 100));
        DataSourceAccessException lastFailure = null;
        for (String category : categories.stream().limit(categoryCount).toList()) {
            try {
                candidates.addAll(fetchCategory(category, hits));
            } catch (DataSourceAccessException exception) {
                lastFailure = exception;
                if (exception.hasStatus(401) || exception.hasStatus(403) || exception.hasStatus(429)) break;
            }
        }
        if (candidates.isEmpty() && lastFailure != null) throw lastFailure;
        return candidates;
    }

    private List<TrendCandidate> fetchCategory(String category, int hits) {
        String query = JapaneseCategoryQueries.forCategory(category);
        DataSourceAccessException rankingFailure = null;
        try {
            String rankingUrl = external.yahooShoppingHighRatingRankingUrl(query, hits).orElseThrow();
            awaitRequestSlot();
            List<TrendCandidate> ranked = parseRanking(external.get(rankingUrl), category);
            if (!ranked.isEmpty()) return ranked;
        } catch (DataSourceAccessException exception) {
            rankingFailure = exception;
            if (exception.hasStatus(401) || exception.hasStatus(403) || exception.hasStatus(429)) throw exception;
        }

        try {
            String searchUrl = external.yahooShoppingSearchUrl(query, Math.min(hits, 50)).orElseThrow();
            awaitRequestSlot();
            return parse(external.get(searchUrl), category);
        } catch (DataSourceAccessException searchFailure) {
            if (rankingFailure != null) searchFailure.addSuppressed(rankingFailure);
            throw searchFailure;
        }
    }

    List<TrendCandidate> parseRanking(String body, String category) {
        try {
            JsonNode items = json.readTree(body)
                .path("high_rating_trend_ranking")
                .path("ranking_data");
            List<TrendCandidate> candidates = new ArrayList<>();
            int position = 0;
            for (JsonNode ranked : items) {
                position++;
                int rank = Math.max(1, ranked.path("rank").asInt(position));
                JsonNode item = ranked.path("item_information");
                String title = item.path("name").asText("").trim();
                BigDecimal price = firstPositivePrice(item);
                if (title.isBlank() || price.signum() <= 0) continue;
                long reviews = ranked.path("review").path("count").asLong(0);
                double rating = ranked.path("review").path("rate").asDouble(0);
                double rankSignal = 101D - Math.min(rank, 100);
                double heat = Math.min(100, rankSignal + Math.log10(reviews + 1) + rating / 2D);
                double volumeSignal = rankSignal + Math.log1p(reviews);
                double amountSignal = volumeSignal * price.doubleValue();
                candidates.add(new TrendCandidate(
                    category, title, title, JapaneseCategoryQueries.forCategory(category), "Yahoo! Japan Shopping",
                    item.path("url").asText("https://shopping.yahoo.co.jp/"),
                    ranked.path("image").path("medium").asText(ranked.path("image").path("small").asText(null)),
                    round(heat), volumeSignal, amountSignal, 50D, price, "JPY",
                    "Yahoo official high-rating trend rank #" + rank
                        + "; the list combines shopper and review signals without exposing actual sales; reviews="
                        + reviews + ", rating=" + rating + "."
                ));
            }
            return candidates;
        } catch (Exception exception) {
            throw new DataSourceAccessException("Yahoo Shopping 趋势榜解析失败：" + exception.getMessage(), exception);
        }
    }

    List<TrendCandidate> parse(String body, String category) {
        try {
            JsonNode hits = json.readTree(body).path("hits");
            List<TrendCandidate> candidates = new ArrayList<>();
            int position = 0;
            for (JsonNode item : hits) {
                position++;
                String title = item.path("name").asText("").trim();
                BigDecimal price = item.path("price").decimalValue();
                if (title.isBlank() || price.signum() <= 0) continue;
                long reviews = item.path("review").path("count").asLong(0);
                double rating = item.path("review").path("rate").asDouble(0);
                double heat = Math.min(100, 96 - position + Math.log10(reviews + 1) * 2 + rating);
                double volumeSignal = reviews > 0 ? Math.log1p(reviews) : 1D / position;
                double amountSignal = volumeSignal * price.doubleValue();
                String image = item.path("exImage").path("url").asText(item.path("image").path("medium").asText(null));
                candidates.add(new TrendCandidate(
                    category, title, title, JapaneseCategoryQueries.forCategory(category), "Yahoo! Japan Shopping",
                    item.path("url").asText("https://shopping.yahoo.co.jp/"), image, round(heat),
                    volumeSignal, amountSignal, 50D, price, "JPY",
                    "Yahoo Japan review rank #" + position + "; reviews=" + reviews + ", rating=" + rating + "."
                ));
            }
            return candidates;
        } catch (Exception exception) {
            throw new DataSourceAccessException("Yahoo Shopping 商品解析失败：" + exception.getMessage(), exception);
        }
    }

    private BigDecimal firstPositivePrice(JsonNode item) {
        for (String field : List.of("bargain_price", "regular_price", "premium_price", "list_price")) {
            BigDecimal value = item.path(field).decimalValue();
            if (value.signum() > 0) return value;
        }
        return BigDecimal.ZERO;
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
            throw new DataSourceAccessException("Yahoo Shopping 请求等待被中断", exception);
        }
    }

    private double round(double value) { return Math.round(value * 10D) / 10D; }
}
