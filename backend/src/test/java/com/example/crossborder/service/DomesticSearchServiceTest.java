package com.example.crossborder.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.crossborder.model.DomesticLink;
import com.example.crossborder.model.SupplierSiteConfig;
import com.example.crossborder.model.TrendCandidate;
import com.example.crossborder.util.SupplierSearchUrlCodec;
import java.math.BigDecimal;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.Charset;
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
        Charset gb18030 = Charset.forName("GB18030");
        String decoded = URLDecoder.decode(links.get(0).url(), gb18030);
        assertTrue(decoded.contains("儿童玩具收纳盒"));
        assertFalse(decoded.contains("おもちゃ"));
        assertTrue(links.get(0).url().contains(URLEncoder.encode("儿童玩具收纳盒", gb18030)));
        assertTrue(links.get(0).note().contains("GB18030/GBK"));
    }

    @Test
    void normalizesUtf8LinksFromHistoricalReportsFor1688() {
        String query = "儿童手表,智能手表,儿童相机,游戏";
        DomesticLink historical = new DomesticLink(
            1, 2, "1688", query + " - 1688搜索",
            "https://s.1688.com/selloffer/offer_search.htm?keywords="
                + URLEncoder.encode(query, StandardCharsets.UTF_8),
            BigDecimal.TEN, "使用 UTF-8 中文采购词跳转搜索"
        );

        DomesticLink normalized = SupplierSearchUrlCodec.normalizeHistorical(historical);

        assertTrue(normalized.url().contains(URLEncoder.encode(query, Charset.forName("GB18030"))));
        assertEquals(query, URLDecoder.decode(
            normalized.url().substring(normalized.url().indexOf("keywords=") + "keywords=".length()),
            Charset.forName("GB18030")
        ));
        assertTrue(normalized.note().contains("GB18030/GBK"));
    }
}
