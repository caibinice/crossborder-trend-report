package com.example.crossborder.service;

import com.example.crossborder.model.AdminSettings;
import com.example.crossborder.model.TrendCandidate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ExternalTrendDataSource implements TrendDataSource {
    private final ExternalDataSourceService sources;
    private final ObjectMapper json;

    public ExternalTrendDataSource(ExternalDataSourceService sources, ObjectMapper json) { this.sources = sources; this.json = json; }

    @Override
    public List<TrendCandidate> fetch(LocalDate date, AdminSettings settings) {
        List<TrendCandidate> candidates = new ArrayList<>();
        for (String category : settings.categories().stream().limit(5).toList()) {
            sources.rainforestSearchUrl(category).flatMap(sources::tryGet).ifPresent(body -> candidates.addAll(parseRainforest(body, category)));
        }
        return candidates;
    }

    List<TrendCandidate> parseRainforest(String body, String category) {
        try {
            JsonNode products = json.readTree(body).path("search_results");
            List<TrendCandidate> out = new ArrayList<>();
            for (JsonNode product : products) {
                String title = product.path("title").asText("");
                if (title.isBlank()) continue;
                BigDecimal price = product.path("price").path("value").decimalValue();
                if (price.signum() <= 0) price = BigDecimal.ZERO;
                double score = Math.max(0, 100 - product.path("position").asDouble(100)) + product.path("ratings_total").asDouble(0) / 100;
                out.add(new TrendCandidate(category, title, title, title, "Amazon/Rainforest", product.path("link").asText(""), score, price, "Amazon search result"));
            }
            return out;
        } catch (Exception ignored) {
            return List.of();
        }
    }
}
