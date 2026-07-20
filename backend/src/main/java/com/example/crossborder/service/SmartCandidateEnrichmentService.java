package com.example.crossborder.service;

import com.example.crossborder.config.AiProperties;
import com.example.crossborder.model.AdminSettings;
import com.example.crossborder.model.SourceTestResult;
import com.example.crossborder.model.TrendCandidate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
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
        if (candidates.isEmpty()) return candidates;
        List<TrendCandidate> output = new ArrayList<>(candidates);
        if (settings.smartMode() && external.aiConfigured()) {
            int batchSize = 25;
            for (int offset = 0; offset < output.size(); offset += batchSize) {
                enrichSafely(output, offset, Math.min(offset + batchSize, output.size()), settings.categories());
            }
        }
        return output.stream().map(this::ensureChineseFallback).toList();
    }

    public SourceTestResult test() {
        if (!external.aiConfigured()) {
            return new SourceTestResult("deepseek", false, 0, "DeepSeek API Key 未配置或 AI 标准化已关闭", Instant.now());
        }
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("model", value(properties.model(), "deepseek-v4-pro"));
            payload.put("stream", false);
            payload.put("max_tokens", 256);
            applyReasoning(payload);
            payload.put("messages", List.of(Map.of("role", "user", "content", "只回复 OK")));
            String response = external.postJson(
                value(properties.baseUrl(), "https://api.deepseek.com").replaceAll("/+$", "") + "/chat/completions",
                json.writeValueAsString(payload),
                Map.of("Authorization", "Bearer " + properties.apiKey()),
                timeout()
            );
            String content = json.readTree(response).path("choices").path(0).path("message").path("content").asText("").trim();
            if (content.isBlank()) throw new DataSourceAccessException("DeepSeek 返回了空内容");
            return new SourceTestResult(
                "deepseek", true, 1, "连接成功，模型=" + value(properties.model(), "deepseek-v4-pro")
                    + "，Thinking=" + properties.thinkingEnabled() + "，effort=" + reasoningEffort(), Instant.now()
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
                source.put("index", index - start);
                source.put("title", item.productNameJp());
                source.put("sourceCategory", item.category());
                source.put("source", item.sourcePlatform());
                source.put("sourceHeat", item.heatScore());
                source.put("sourcePrice", item.sourcePrice());
                source.put("sourceCurrency", item.sourceCurrency());
                source.put("evidence", item.reason());
                sourceItems.add(source);
            }
            String categoryText = String.join("、", categories == null ? List.of() : categories);
            String system = "你是跨境电商商品翻译与选品评估器。只根据输入标题、来源、价格和证据评估，不编造真实销量或销售额。"
                + "返回严格 JSON 对象 {\"items\":[{\"index\":0,\"nameCn\":\"\",\"category\":\"\",\"keywords\":\"\",\"reason\":\"\",\"aiScore\":50}]}。"
                + "nameCn 必须是简洁的简体中文商品名，不得包含日文假名；category 必须从这些品类选择：" + categoryText
                + "；keywords 必须是 2-4 个简体中文采购搜索词，不得出现日文假名；"
                + "aiScore 为 1-100 的跨境销售潜力评分，综合需求普适性、差异化、物流友好度和证据质量；"
                + "reason 不超过 80 字，明确说明评分依据并保留输入中的排名、评论或评分事实。";
            String user = json.writeValueAsString(Map.of("items", sourceItems));
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("model", value(properties.model(), "deepseek-v4-pro"));
            payload.put("stream", false);
            payload.put("max_tokens", 12000);
            payload.put("response_format", Map.of("type", "json_object"));
            applyReasoning(payload);
            payload.put("messages", List.of(
                Map.of("role", "system", "content", system),
                Map.of("role", "user", "content", user)
            ));
            String response = external.postJson(
                value(properties.baseUrl(), "https://api.deepseek.com").replaceAll("/+$", "") + "/chat/completions",
                json.writeValueAsString(payload),
                Map.of("Authorization", "Bearer " + properties.apiKey()),
                timeout()
            );
            String content = json.readTree(response).path("choices").path(0).path("message").path("content").asText("");
            JsonNode items = json.readTree(stripFence(content)).path("items");
            if (!items.isArray() || items.isEmpty()) throw new DataSourceAccessException("DeepSeek 没有返回商品数组");
            Map<Integer, JsonNode> byIndex = new HashMap<>();
            for (JsonNode item : items) byIndex.put(item.path("index").asInt(-1), item);
            if (byIndex.size() < end - start) {
                throw new DataSourceAccessException("DeepSeek 仅返回 " + byIndex.size() + "/" + (end - start) + " 条商品");
            }
            for (int index = start; index < end; index++) {
                JsonNode enriched = byIndex.get(index - start);
                if (enriched == null) continue;
                TrendCandidate original = output.get(index);
                // The source adapter already queried a configured category. AI may improve names and
                // procurement terms, but reclassification would break the configured per-category quota.
                String category = original.category();
                String nameCn = chineseText(enriched.path("nameCn").asText(""), original.productNameCn(), category + "热销商品");
                String keywords = chineseText(enriched.path("keywords").asText(""), nameCn, category);
                output.set(index, new TrendCandidate(
                    category, original.productNameJp(), nameCn, keywords,
                    original.sourcePlatform(), original.sourceUrl(), original.imageUrl(), original.heatScore(),
                    original.salesVolumeScore(), original.salesAmountScore(), score(enriched.path("aiScore"), original.aiScore()),
                    original.sourcePrice(), original.sourceCurrency(), text(enriched, "reason", original.reason())
                ));
            }
        } catch (Exception exception) {
            throw new DataSourceAccessException("DeepSeek 响应解析失败：" + exception.getMessage(), exception);
        }
    }

    private void enrichSafely(List<TrendCandidate> output, int start, int end, List<String> categories) {
        try {
            enrichBatch(output, start, end, categories);
        } catch (RuntimeException exception) {
            if (end - start > 6) {
                int middle = start + (end - start) / 2;
                enrichSafely(output, start, middle, categories);
                enrichSafely(output, middle, end, categories);
                return;
            }
            log.warn("AI 商品翻译评分失败，{} 条商品使用中文品类兜底: {}", end - start, rootMessage(exception));
        }
    }

    private TrendCandidate ensureChineseFallback(TrendCandidate original) {
        String category = original.category() == null || original.category().isBlank() ? "跨境" : original.category().trim();
        String nameCn = chineseText(original.productNameCn(), "", category + "热销商品");
        String keywords = chineseText(original.keywords(), nameCn, category + " 热销商品");
        if (nameCn.equals(original.productNameCn()) && keywords.equals(original.keywords())) return original;
        return new TrendCandidate(
            original.category(), original.productNameJp(), nameCn, keywords, original.sourcePlatform(), original.sourceUrl(),
            original.imageUrl(), original.heatScore(), original.salesVolumeScore(), original.salesAmountScore(), original.aiScore(),
            original.sourcePrice(), original.sourceCurrency(), original.reason()
        );
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

    private String chineseText(String candidate, String fallback, String finalFallback) {
        String value = candidate == null ? "" : candidate.replaceAll("\\s+", " ").trim();
        if (isChineseSearchText(value)) return abbreviate(value, 120);
        String second = fallback == null ? "" : fallback.replaceAll("\\s+", " ").trim();
        if (isChineseSearchText(second)) return abbreviate(second, 120);
        return finalFallback;
    }

    private boolean isChineseSearchText(String value) {
        return value != null && !value.isBlank() && !containsJapaneseKana(value)
            && value.matches(".*[\\p{IsHan}].*");
    }

    private boolean containsJapaneseKana(String value) {
        return value.matches(".*[\\p{InHiragana}\\p{InKatakana}].*");
    }

    private double score(JsonNode node, double fallback) {
        double value = node.isNumber() ? node.asDouble() : fallback;
        return Math.round(Math.max(1D, Math.min(100D, value)) * 10D) / 10D;
    }

    private String abbreviate(String value, int max) {
        return value.length() <= max ? value : value.substring(0, max);
    }

    private void applyReasoning(Map<String, Object> payload) {
        if (properties.thinkingEnabled()) {
            payload.put("thinking", Map.of("type", "enabled"));
            payload.put("reasoning_effort", reasoningEffort());
        } else {
            payload.put("thinking", Map.of("type", "disabled"));
            payload.put("temperature", 0);
        }
    }

    private String reasoningEffort() {
        return "max".equalsIgnoreCase(properties.reasoningEffort()) ? "max" : "high";
    }

    private Duration timeout() {
        return Duration.ofSeconds(Math.max(30, Math.min(properties.timeoutSeconds(), 300)));
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
