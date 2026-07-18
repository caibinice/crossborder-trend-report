package com.example.crossborder.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import com.example.crossborder.model.TrendCandidate;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class WooCommerceTrendSourceTest {
    @Test
    void parsesPublicStoreProductPriceImageAndEvidence() {
        String response = """
            [{
              "name":"Matcha &amp; Chocolate Gift",
              "permalink":"https://shop.test/product/matcha",
              "short_description":"<p>Popular Japanese snack box.</p>",
              "average_rating":"4.7",
              "review_count":23,
              "prices":{"price":"1299","currency_code":"USD","currency_minor_unit":2},
              "categories":[{"name":"Japanese Food"}],
              "images":[{"src":"https://shop.test/matcha.jpg"}]
            }]
            """;
        WooCommerceTrendSource source = new WooCommerceTrendSource(
            mock(ExternalDataSourceService.class), new ObjectMapper()
        );

        List<TrendCandidate> candidates = source.parse(response, "https://shop.test");

        assertEquals(1, candidates.size());
        TrendCandidate candidate = candidates.get(0);
        assertEquals("Matcha & Chocolate Gift", candidate.productNameJp());
        assertEquals("食品", candidate.category());
        assertEquals("USD", candidate.sourceCurrency());
        assertEquals(0, new BigDecimal("12.99").compareTo(candidate.sourcePrice()));
        assertEquals("https://shop.test/matcha.jpg", candidate.imageUrl());
        assertTrue(candidate.reason().contains("评论=23"));
    }
}
