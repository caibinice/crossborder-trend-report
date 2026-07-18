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
public class RainforestTrendSource {
    private final ExternalDataSourceService external;
    private final ObjectMapper json;

    public RainforestTrendSource(ExternalDataSourceService external, ObjectMapper json) {
        this.external = external;
        this.json = json;
    }

    public List<TrendCandidate> fetch(AdminSettings settings) {
        if (!external.rainforestConfigured()) return List.of();
        List<TrendCandidate> candidates = new ArrayList<>();
        for (String category : settings.categories().stream().limit(5).toList()) {
            String url = external.rainforestSearchUrl(JapaneseCategoryQueries.forCategory(category)).orElseThrow();
            candidates.addAll(parse(external.get(url), category));
        }
        return candidates;
    }

    List<TrendCandidate> parse(String body, String category) {
        try {
            JsonNode products = json.readTree(body).path("search_results");
            List<TrendCandidate> candidates = new ArrayList<>();
            for (JsonNode product : products) {
                String title = product.path("title").asText("").trim();
                if (title.isBlank()) continue;
                BigDecimal price = product.path("price").path("value").decimalValue();
                if (price.signum() <= 0) continue;
                double score = Math.max(0, 100 - product.path("position").asDouble(100))
                    + Math.log10(product.path("ratings_total").asDouble(0) + 1) * 2;
                candidates.add(new TrendCandidate(
                    category, title, title, JapaneseCategoryQueries.forCategory(category), "Amazon JP / Rainforest",
                    product.path("link").asText(""), product.path("image").asText(null), Math.min(100, score), price, "JPY",
                    "Amazon 搜索排名=" + product.path("position").asInt(0) + "，评论=" + product.path("ratings_total").asLong(0) + "。"
                ));
            }
            return candidates;
        } catch (Exception exception) {
            throw new DataSourceAccessException("Rainforest 商品解析失败：" + exception.getMessage(), exception);
        }
    }
}
