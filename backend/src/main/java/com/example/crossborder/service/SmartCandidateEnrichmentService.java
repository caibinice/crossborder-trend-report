package com.example.crossborder.service;

import com.example.crossborder.config.AiProperties;
import com.example.crossborder.model.AdminSettings;
import com.example.crossborder.model.SourceTestResult;
import com.example.crossborder.model.TrendCandidate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class SmartCandidateEnrichmentService {
    private static final Logger log = LoggerFactory.getLogger(SmartCandidateEnrichmentService.class);
    private final AiProperties properties;
    private final ExternalDataSourceService external;
    private final ObjectMapper json;

    public SmartCandidateEnrichmentService(AiProperties properties, ExternalDataSourceService external, ObjectMapper json) {
        this.properties = properties;
        this.external = external;
        this.json = json;
    }

    public List<TrendCandidate> enrich(List<TrendCandidate> candidates, AdminSettings settings) {
        if (!settings.smartMode() || !external.aiConfigured() || candidates.isEmpty()) return candidates;
        List<TrendCandidate> output = new ArrayList<>(candidates);
        int batchSize = 20;
        for (int offset = 0; offset < output.size(); offset += batchSize) {
            int end = Math.min(offset + batchSize, output.size());
            try {
                enrichBatch(output, offset, end, settings.categories());
            } catch (RuntimeException exception) {
                log.warn("AI 商品标准化失败，保留原始数据: {}", rootMessage(exception));
            }
        }
        return List.copyOf(output);
    }

    public SourceTestResult test() {
        if (!external.aiConfigured()) {
            return new SourceTestResult("deepseek", false, 0, "DeepSeek API Key 未配置或 AI 标准化已关闭", Instant.now());
        }
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("model", value(properties.model(), "deepseek-chat"));
            payload.put("temperature", 0);
            payload.put("stream", false);
            payload.put("max_tokens", 8);
            payload.put("messages", List.of(Map.of("role", "user", "content", "只回复 OK")));
            String response = external.postJson(
                value(properties.baseUrl(), "https://api.deepseek.com").replaceAll("/+$", "") + "/chat/completions",
                json.writeValueAsString(payload),
                Map.of("Authorization", "Bearer " + properties.apiKey())
            );
            String content = json.readTree(response).path("choices").path(0).path("message").path("content").asText("").trim();
            if (content.isBlank()) throw new DataSourceAccessException("DeepSeek 返回了空内容");
            return new SourceTestResult(
                "deepseek", true, 1, "连接成功，模型=" + value(properties.model(), "deepseek-chat"), Instant.now()
            );
        } catch (Exception exception) {
            return new SourceTestResult("deepseek", false, 0, rootMessage(exception), Instant.now());
        }
    }

    private void enrichBatch(List<TrendCandidate> output, int start, int end, List<String> categories) {
        try {
            List<Map<String, Object>> sourceItems = new ArrayList<>();
            for (int index = start; index < end; index++) {
                TrendCandidate item = output.get(index);
                Map<String, Object> source = new LinkedHashMap<>();
                source.put("index", index);
                source.put("title", item.productNameJp());
                source.put("sourceCategory", item.category());
                source.put("source", item.sourcePlatform());
                source.put("heat", item.heatScore());
                source.put("evidence", item.reason());
                sourceItems.add(source);
            }
            String categoryText = String.join("、", categories == null ? List.of() : categories);
            String system = "你是跨境电商商品数据标准化器。只根据输入事实处理，不编造销量。"
                + "返回严格 JSON 对象 {\"items\":[{\"index\":0,\"nameCn\":\"\",\"category\":\"\",\"keywords\":\"\",\"reason\":\"\"}]}。"
                + "nameCn 为简洁中文商品名；category 优先从这些品类选择：" + categoryText
                + "；keywords 为 2-4 个中文采购词；reason 不超过 60 字并保留排名/评分等输入依据。";
            String user = json.writeValueAsString(Map.of("items", sourceItems));
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("model", value(properties.model(), "deepseek-chat"));
            payload.put("temperature", 0.1);
            payload.put("stream", false);
            payload.put("response_format", Map.of("type", "json_object"));
            payload.put("messages", List.of(
                Map.of("role", "system", "content", system),
                Map.of("role", "user", "content", user)
            ));
            String response = external.postJson(
                value(properties.baseUrl(), "https://api.deepseek.com").replaceAll("/+$", "") + "/chat/completions",
                json.writeValueAsString(payload),
                Map.of("Authorization", "Bearer " + properties.apiKey())
            );
            String content = json.readTree(response).path("choices").path(0).path("message").path("content").asText("");
            JsonNode items = json.readTree(stripFence(content)).path("items");
            Map<Integer, JsonNode> byIndex = new HashMap<>();
            for (JsonNode item : items) byIndex.put(item.path("index").asInt(-1), item);
            for (int index = start; index < end; index++) {
                JsonNode enriched = byIndex.get(index);
                if (enriched == null) continue;
                TrendCandidate original = output.get(index);
                output.set(index, new TrendCandidate(
                    text(enriched, "category", original.category()), original.productNameJp(),
                    text(enriched, "nameCn", original.productNameCn()), text(enriched, "keywords", original.keywords()),
                    original.sourcePlatform(), original.sourceUrl(), original.imageUrl(), original.heatScore(),
                    original.sourcePrice(), original.sourceCurrency(), text(enriched, "reason", original.reason())
                ));
            }
        } catch (Exception exception) {
            throw new DataSourceAccessException("DeepSeek 响应解析失败：" + exception.getMessage(), exception);
        }
    }

    private String stripFence(String value) {
        String text = value == null ? "" : value.trim();
        if (text.startsWith("```")) {
            text = text.replaceFirst("^```(?:json)?\\s*", "").replaceFirst("\\s*```$", "");
        }
        return text;
    }

    private String text(JsonNode node, String key, String fallback) {
        String value = node.path(key).asText("").trim();
        return value.isBlank() ? fallback : value;
    }

    private String value(String input, String fallback) {
        return input == null || input.isBlank() ? fallback : input.trim();
    }

    private String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) current = current.getCause();
        return current.getMessage() == null ? throwable.getClass().getSimpleName() : current.getMessage();
    }
}
