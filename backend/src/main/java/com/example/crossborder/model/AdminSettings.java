package com.example.crossborder.model;
import java.math.BigDecimal;
import java.util.List;
public record AdminSettings(
    List<String> foreignSources,
    List<String> domesticSources,
    List<String> categories,
    List<String> regions,
    String sourceMode,
    String frequencyCron,
    int maxProducts,
    BigDecimal jpyCnyRate,
    boolean autoExchangeRate,
    BigDecimal defaultShippingCny,
    boolean smartMode,
    int maxCategories,
    int productsPerCategory,
    String rankingMetric,
    List<SupplierSiteConfig> supplierSites
) {
    public AdminSettings(
        List<String> foreignSources,
        List<String> domesticSources,
        List<String> categories,
        List<String> regions,
        String sourceMode,
        String frequencyCron,
        int maxProducts,
        BigDecimal jpyCnyRate,
        boolean autoExchangeRate,
        BigDecimal defaultShippingCny,
        boolean smartMode
    ) {
        this(
            foreignSources, domesticSources, categories, regions, sourceMode, frequencyCron, maxProducts,
            jpyCnyRate, autoExchangeRate, defaultShippingCny, smartMode,
            Math.max(1, Math.min(categories == null ? 1 : categories.size(), 10)),
            Math.max(1, Math.min(30, maxProducts / Math.max(1, Math.min(categories == null ? 1 : categories.size(), 10)))),
            "sales_volume",
            List.of(
                new SupplierSiteConfig("1688", "https://s.1688.com/selloffer/offer_search.htm?keywords={keyword}"),
                new SupplierSiteConfig("淘宝", "https://s.taobao.com/search?q={keyword}"),
                new SupplierSiteConfig("拼多多", "https://mobile.yangkeduo.com/search_result.html?search_key={keyword}")
            )
        );
    }
}
