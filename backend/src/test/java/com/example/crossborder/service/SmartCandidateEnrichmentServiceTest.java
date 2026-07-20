package com.example.crossborder.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.crossborder.config.AiProperties;
import com.example.crossborder.model.AdminSettings;
import com.example.crossborder.model.TrendCandidate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class SmartCandidateEnrichmentServiceTest {
    @Test
    void usesV4ProThinkingHighAndReturnsChineseProcurementKeywords() throws Exception {
        ObjectMapper json = new ObjectMapper();
        ExternalDataSourceService external = mock(ExternalDataSourceService.class);
        when(external.aiConfigured()).thenReturn(true);
        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        String content = json.writeValueAsString(Map.of("items", List.of(Map.of(
            "index", 0, "nameCn", "儿童玩具收纳盒", "category", "家居", "keywords", "儿童 玩具 收纳盒",
            "reason", "评论证据充分，适合跨境运输", "aiScore", 88
        ))));
        String response = json.writeValueAsString(Map.of("choices", List.of(Map.of("message", Map.of("content", content)))));
        when(external.postJson(anyString(), body.capture(), anyMap(), any(Duration.class))).thenReturn(response);
        SmartCandidateEnrichmentService service = new SmartCandidateEnrichmentService(
            new AiProperties(true, "https://api.deepseek.com", "secret", "deepseek-v4-pro", true, "high", 90),
            external, json
        );
        AdminSettings settings = new AdminSettings(
            List.of("Rakuten"), List.of("1688"), List.of("玩具"), List.of("日本"), "external",
            "0 0 8 * * *", 10, new BigDecimal("0.05"), true, new BigDecimal("18"), true
        );
        TrendCandidate source = new TrendCandidate(
            "玩具", "おもちゃ収納", "おもちゃ収納", "おもちゃ 人気", "Rakuten Ichiba",
            "https://example.com", null, 80, 10, 20, 50, new BigDecimal("1000"), "JPY", "评论=100"
        );

        TrendCandidate result = service.enrich(List.of(source), settings).get(0);
        JsonNode request = json.readTree(body.getValue());

        assertEquals("deepseek-v4-pro", request.path("model").asText());
        assertEquals("enabled", request.path("thinking").path("type").asText());
        assertEquals("high", request.path("reasoning_effort").asText());
        assertFalse(request.has("temperature"));
        assertEquals("玩具", result.category());
        assertEquals("儿童 玩具 收纳盒", result.keywords());
        assertEquals(88D, result.aiScore());
    }
}
