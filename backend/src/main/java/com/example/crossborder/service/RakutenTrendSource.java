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
    private final ExternalDataSourceService external;
    private final ObjectMapper json;

    public RakutenTrendSource(ExternalDataSourceService external, ObjectMapper json) {
        this.external = external;
        this.json = json;
    }

    public List<TrendCandidate> fetch(AdminSettings settings) {
        if (!external.rakutenConfigured()) return List.of();
        List<TrendCandidate> candidates = new ArrayList<>();
        int categoryCount = Math.min(Math.max(settings.categories().size(), 1), 5);
        int hits = Math.max(3, Math.min(10, settings.maxProducts() / categoryCount + 1));
        for (String category : settings.categories().stream().limit(categoryCount).toList()) {
            String url = external.rakutenSearchUrl(JapaneseCategoryQueries.forCategory(category), hits).orElseThrow();
            candidates.addAll(parse(external.get(url), category));
        }
        return candidates;
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
                candidates.add(new TrendCandidate(
                    category, title, title, JapaneseCategoryQueries.forCategory(category), "Rakuten Ichiba",
                    item.path("itemUrl").asText("https://www.rakuten.co.jp/"), image(item), round(heat), price, "JPY",
                    "乐天评论热度排序第 " + position + "；评论=" + reviews + "，评分=" + rating + "。"
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
