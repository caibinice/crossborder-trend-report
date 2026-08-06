package com.example.crossborder.service;

import static org.junit.jupiter.api.Assertions.assertIterableEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class MarketCatalogTest {
    @Test
    void keepsJapanCategoriesUnchanged() {
        List<String> configured = List.of("玩具", "家居", "美妆");

        assertIterableEquals(configured, MarketCatalog.categories(configured, "jp"));
    }

    @Test
    void normalizesLegacyEnglishCategoriesToChineseForOtherMarkets() {
        List<String> configured = List.of("Toys", "Home & Living", "Beauty", "Beading");

        assertIterableEquals(
            List.of("玩具", "家居", "美妆", "串珠"),
            MarketCatalog.categories(configured, "sea")
        );
    }
}
