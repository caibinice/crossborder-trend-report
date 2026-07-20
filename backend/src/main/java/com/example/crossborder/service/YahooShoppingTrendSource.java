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
    private final ExternalDataSourceService external;
    private final ObjectMapper json;

    public YahooShoppingTrendSource(ExternalDataSourceService external, ObjectMapper json) {
        this.external = external;
        this.json = json;
    }

    public List<TrendCandidate> fetch(AdminSettings settings) {
        if (!external.yahooConfigured()) return List.of();
        List<TrendCandidate> candidates = new ArrayList<>();
        int categoryCount = Math.min(Math.max(settings.categories().size(), 1), Math.max(1, settings.maxCategories()));
        int hits = Math.max(1, Math.min(50, settings.productsPerCategory()));
        for (String category : settings.categories().stream().limit(categoryCount).toList()) {
            String url = external.yahooShoppingSearchUrl(JapaneseCategoryQueries.forCategory(category), hits).orElseThrow();
            candidates.addAll(parse(external.get(url), category));
        }
        return candidates;
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
                    "Yahoo 日本购物评论排序第 " + position + "；评论=" + reviews + "，评分=" + rating + "。"
                ));
            }
            return candidates;
        } catch (Exception exception) {
            throw new DataSourceAccessException("Yahoo Shopping 商品解析失败：" + exception.getMessage(), exception);
        }
    }

    private double round(double value) { return Math.round(value * 10D) / 10D; }
}
