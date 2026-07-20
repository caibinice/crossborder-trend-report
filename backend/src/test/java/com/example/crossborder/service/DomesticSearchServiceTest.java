package com.example.crossborder.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.crossborder.model.DomesticLink;
import com.example.crossborder.model.SupplierSiteConfig;
import com.example.crossborder.model.TrendCandidate;
import java.math.BigDecimal;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

class DomesticSearchServiceTest {
    @Test
    void replacesJapaneseKeywordsWithChineseNameAndUsesConfiguredSites() {
        TrendCandidate candidate = new TrendCandidate(
            "玩具", "おもちゃ収納", "儿童玩具收纳盒", "おもちゃ 人気", "Rakuten Ichiba",
            "https://example.com", null, 80, 10, 20, 70, new BigDecimal("1000"), "JPY", "测试"
        );

        List<DomesticLink> links = new DomesticSearchService().search(
            candidate, new BigDecimal("50"),
            List.of(new SupplierSiteConfig("1688", "https://s.1688.com/search?keywords={keyword}"))
        );

        assertEquals(1, links.size());
        String decoded = URLDecoder.decode(links.get(0).url(), StandardCharsets.UTF_8);
        assertTrue(decoded.contains("儿童玩具收纳盒"));
        assertFalse(decoded.contains("おもちゃ"));
    }
}
