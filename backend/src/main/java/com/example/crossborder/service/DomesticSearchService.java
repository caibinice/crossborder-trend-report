package com.example.crossborder.service;

import com.example.crossborder.model.DomesticLink;
import com.example.crossborder.model.SupplierSiteConfig;
import com.example.crossborder.model.TrendCandidate;
import com.example.crossborder.util.SupplierSearchUrlCodec;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class DomesticSearchService {
    public List<DomesticLink> search(
        TrendCandidate candidate, BigDecimal sourcePriceCny, List<SupplierSiteConfig> configuredSites
    ) {
        BigDecimal base = estimate(candidate.category(), sourcePriceCny);
        String query = procurementQuery(candidate);
        List<SupplierSiteConfig> sites = configuredSites == null || configuredSites.isEmpty()
            ? defaultSites()
            : configuredSites;
        List<DomesticLink> links = new ArrayList<>();
        for (SupplierSiteConfig site : sites) {
            String encoded = SupplierSearchUrlCodec.encodeKeyword(site, query);
            BigDecimal price = base.multiply(multiplier(site.name())).setScale(2, RoundingMode.HALF_UP);
            links.add(new DomesticLink(
                0, 0, site.name(), query + " - " + site.name() + "搜索",
                site.urlTemplate().replace("{keyword}", encoded), price,
                "使用 " + SupplierSearchUrlCodec.encodingLabel(site) + " 中文采购词“" + query
                    + "”跳转搜索；价格为估算，需以平台实时报价为准。"
            ));
        }
        return List.copyOf(links);
    }

    String procurementQuery(TrendCandidate candidate) {
        String keywords = clean(candidate.keywords());
        if (isChinese(keywords)) return abbreviate(keywords, 80);
        String chineseName = clean(candidate.productNameCn());
        if (isChinese(chineseName)) return abbreviate(chineseName, 80);
        String category = clean(candidate.category());
        return category.isBlank() ? "跨境热销商品" : category + " 热销商品";
    }

    private List<SupplierSiteConfig> defaultSites() {
        return List.of(
            new SupplierSiteConfig("1688", "https://s.1688.com/selloffer/offer_search.htm?keywords={keyword}"),
            new SupplierSiteConfig("淘宝", "https://s.taobao.com/search?q={keyword}"),
            new SupplierSiteConfig("拼多多", "https://mobile.yangkeduo.com/search_result.html?search_key={keyword}")
        );
    }

    private boolean containsJapaneseKana(String value) {
        return value.matches(".*[\\p{InHiragana}\\p{InKatakana}].*");
    }

    private boolean isChinese(String value) {
        return value != null && !value.isBlank() && !containsJapaneseKana(value) && value.matches(".*[\\p{IsHan}].*");
    }

    private String clean(String value) {
        return (value == null ? "" : value)
            .replaceAll("[\\p{Cntrl}\\p{Cf}]", " ")
            .replaceAll("[|<>]", " ")
            .replaceAll("\\s+", " ")
            .trim();
    }

    private String abbreviate(String value, int max) {
        return value.length() <= max ? value : value.substring(0, max);
    }

    private BigDecimal multiplier(String platform) {
        String value = platform == null ? "" : platform.toLowerCase(Locale.ROOT);
        if (value.contains("1688")) return BigDecimal.ONE;
        if (value.contains("拼多多") || value.contains("pinduoduo")) return new BigDecimal("1.10");
        if (value.contains("淘宝") || value.contains("taobao")) return new BigDecimal("1.25");
        return new BigDecimal("1.15");
    }

    private BigDecimal estimate(String category, BigDecimal sourcePriceCny) {
        double ratio = switch (category == null ? "" : category) {
            case "玩具" -> 0.30;
            case "家居" -> 0.34;
            case "美妆" -> 0.28;
            case "宠物" -> 0.36;
            case "数码" -> 0.42;
            case "户外" -> 0.38;
            case "母婴" -> 0.33;
            case "汽车" -> 0.35;
            default -> 0.36;
        };
        return sourcePriceCny.multiply(BigDecimal.valueOf(ratio))
            .max(BigDecimal.valueOf(6))
            .setScale(2, RoundingMode.HALF_UP);
    }
}
