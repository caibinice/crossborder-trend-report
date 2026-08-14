package com.example.crossborder.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
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
import java.util.Optional;
import org.junit.jupiter.api.Test;

class YahooShoppingTrendSourceTest {
    @Test
    void buildsCurrentOfficialRankingAndItemSearchUrls() {
        ExternalDataSourceService external = new ExternalDataSourceService(
            sourceProperties("client-id"),
            new AiProperties(false, "", "", "", true, "max", 90)
        );

        String ranking = external.yahooShoppingHighRatingRankingUrl("収納 ボックス", 120).orElseThrow();
        String search = external.yahooShoppingSearchUrl("収納 ボックス", 80).orElseThrow();

        assertTrue(ranking.startsWith(
            "https://shopping.yahooapis.jp/ShoppingWebService/V1/highRatingTrendRanking?"
        ));
        assertTrue(ranking.contains("appid=client-id"));
        assertTrue(ranking.contains("query=%E5%8F%8E%E7%B4%8D+%E3%83%9C%E3%83%83%E3%82%AF%E3%82%B9"));
        assertTrue(ranking.endsWith("&offset=1&limit=100"));

        assertTrue(search.startsWith(
            "https://shopping.yahooapis.jp/ShoppingWebService/V3/itemSearch?"
        ));
        assertTrue(search.contains("results=50"));
        assertTrue(search.contains("sort=-review_count"));
        assertTrue(search.contains("is_cross_border_agency=true"));
        assertFalse(search.contains("limit=100"));
    }

    @Test
    void parsesOfficialHighRatingTrendRankingWithoutClaimingRawSales() {
        String response = """
            {
              "high_rating_trend_ranking": {
                "meta": {"last_modified": "2026-07-26"},
                "ranking_data": [{
                  "rank": 2,
                  "item_information": {
                    "name": "折りたたみ収納ボックス",
                    "code": "shop_item-1",
                    "url": "https://store.shopping.yahoo.co.jp/shop/item-1.html",
                    "regular_price": 2980,
                    "bargain_price": 2680
                  },
                  "image": {
                    "medium": "https://item-shopping.c.yimg.jp/i/g/shop_item-1"
                  },
                  "review": {
                    "rate": 4.7,
                    "count": 321
                  }
                }]
              }
            }
            """;
        YahooShoppingTrendSource source = new YahooShoppingTrendSource(
            mock(ExternalDataSourceService.class), new ObjectMapper()
        );

        List<TrendCandidate> candidates = source.parseRanking(response, "家居");

        assertEquals(1, candidates.size());
        TrendCandidate item = candidates.get(0);
        assertEquals("折りたたみ収納ボックス", item.productNameJp());
        assertEquals(0, new BigDecimal("2680").compareTo(item.sourcePrice()));
        assertEquals("JPY", item.sourceCurrency());
        assertEquals("https://store.shopping.yahoo.co.jp/shop/item-1.html", item.sourceUrl());
        assertEquals("https://item-shopping.c.yimg.jp/i/g/shop_item-1", item.imageUrl());
        assertTrue(item.reason().contains("高评价趋势第 2"));
        assertTrue(item.reason().contains("不公开具体销量"));
        assertTrue(item.salesVolumeScore() > 0);
        assertTrue(item.salesAmountScore() > item.salesVolumeScore());
    }

    @Test
    void previewOnlyRequestsOneCategoryAndThreeRankedProducts() {
        ExternalDataSourceService external = mock(ExternalDataSourceService.class);
        when(external.yahooConfigured()).thenReturn(true);
        when(external.yahooShoppingHighRatingRankingUrl(anyString(), anyInt()))
            .thenReturn(Optional.of("https://example.test/ranking"));
        when(external.get("https://example.test/ranking")).thenReturn("""
            {
              "high_rating_trend_ranking": {
                "ranking_data": [{
                  "rank": 1,
                  "item_information": {
                    "name": "収納ボックス",
                    "url": "https://example.test/item",
                    "regular_price": 1980
                  }
                }]
              }
            }
            """);
        AdminSettings settings = new AdminSettings(
            List.of("Yahoo Shopping"), List.of("1688"), List.of("玩具", "家居", "数码"), List.of("JP"),
            "live", "0 0 8 * * *", 30, new BigDecimal("0.05"), true,
            new BigDecimal("20"), false
        );

        List<TrendCandidate> candidates = new YahooShoppingTrendSource(
            external, new ObjectMapper()
        ).preview(settings);

        assertEquals(1, candidates.size());
        verify(external, times(1)).yahooShoppingHighRatingRankingUrl(anyString(), anyInt());
        verify(external, times(1)).get("https://example.test/ranking");
    }

    private SourceProperties sourceProperties(String yahooClientId) {
        return new SourceProperties(
            true, "JP", true, true, "https://shop.test", "", "", "demo", "", "", "", "",
            "rainforest", "", "", "", "", "", "", "20260701",
            "https://openapi.rakuten.co.jp", yahooClientId, "search-link", ""
        );
    }
}
