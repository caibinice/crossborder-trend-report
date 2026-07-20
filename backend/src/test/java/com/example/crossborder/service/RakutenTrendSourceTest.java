package com.example.crossborder.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.crossborder.config.AiProperties;
import com.example.crossborder.config.SourceProperties;
import com.example.crossborder.model.AdminSettings;
import com.example.crossborder.model.TrendCandidate;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class RakutenTrendSourceTest {
    @Test
    void keepsAccessKeyInHeaderAndAddsOptionalAffiliateId() {
        SourceProperties properties = new SourceProperties(
            true, "JP", true, true, "https://shop.test", "demo", "", "", "", "",
            "rainforest", "", "", "", "app-id", "secret-key", "affiliate-id", "20260701", "", "search-link", ""
        );
        ExternalDataSourceService external = new ExternalDataSourceService(
            properties, new AiProperties(false, "", "", "", true, "high", 90)
        );

        String url = external.rakutenSearchUrl("収納 ボックス", 8).orElseThrow();

        assertTrue(url.startsWith("https://openapi.rakuten.co.jp/ichibams/api/IchibaItem/Search/20260701?"));
        assertTrue(url.contains("applicationId=app-id"));
        assertTrue(url.contains("affiliateId=affiliate-id"));
        assertTrue(url.contains("keyword=%E5%8F%8E%E7%B4%8D+%E3%83%9C%E3%83%83%E3%82%AF%E3%82%B9"));
        assertFalse(url.contains("secret-key"));
        assertFalse(url.contains("accessKey="));
        assertEquals("secret-key", external.rakutenHeaders().get("accessKey"));
    }

    @Test
    void parsesVersionTwoItemsAndPrefersAffiliateLink() {
        String response = """
            {
              "items": [{
                "itemName": "折りたたみ収納ボックス",
                "itemPrice": 2980,
                "itemUrl": "https://item.rakuten.co.jp/shop/item/",
                "affiliateUrl": "https://hb.afl.rakuten.co.jp/example",
                "reviewCount": 321,
                "reviewAverage": 4.6,
                "shopName": "テストショップ",
                "shipOverseasFlag": 1,
                "mediumImageUrls": [{"imageUrl": "https://image.rakuten.co.jp/item.jpg"}]
              }]
            }
            """;
        RakutenTrendSource source = new RakutenTrendSource(
            mock(ExternalDataSourceService.class), new ObjectMapper()
        );

        List<TrendCandidate> candidates = source.parse(response, "家居");

        assertEquals(1, candidates.size());
        TrendCandidate item = candidates.get(0);
        assertEquals("折りたたみ収納ボックス", item.productNameJp());
        assertEquals(0, new BigDecimal("2980").compareTo(item.sourcePrice()));
        assertEquals("JPY", item.sourceCurrency());
        assertEquals("https://hb.afl.rakuten.co.jp/example", item.sourceUrl());
        assertEquals("https://image.rakuten.co.jp/item.jpg", item.imageUrl());
        assertTrue(item.reason().contains("评论=321"));
        assertTrue(item.reason().contains("支持海外配送"));
    }

    @Test
    void previewOnlyRequestsOneCategory() {
        ExternalDataSourceService external = mock(ExternalDataSourceService.class);
        when(external.rakutenConfigured()).thenReturn(true);
        when(external.rakutenSearchUrl(anyString(), anyInt())).thenReturn(Optional.of("https://example.test/search"));
        when(external.rakutenHeaders()).thenReturn(Map.of("accessKey", "secret"));
        when(external.get(anyString(), anyMap())).thenReturn("""
            {"items":[{"itemName":"収納ボックス","itemPrice":1980}]}
            """);
        AdminSettings settings = new AdminSettings(
            List.of("Rakuten"), List.of("1688"), List.of("玩具", "家居", "数码"), List.of("JP"),
            "live", "0 0 8 * * *", 30, new BigDecimal("0.05"), true,
            new BigDecimal("20"), false
        );

        List<TrendCandidate> candidates = new RakutenTrendSource(external, new ObjectMapper()).preview(settings);

        assertEquals(1, candidates.size());
        verify(external, times(1)).get(anyString(), anyMap());
    }
}
