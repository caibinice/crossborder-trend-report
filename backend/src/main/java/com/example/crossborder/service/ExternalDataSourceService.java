package com.example.crossborder.service;

import com.example.crossborder.config.AiProperties;
import com.example.crossborder.config.SourceProperties;
import com.example.crossborder.model.DataSourceStatus;
import java.net.URI;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ExternalDataSourceService {
    private static final String USER_AGENT = "CrossborderTrendReport/1.0 (+data-integration)";

    private final SourceProperties properties;
    private final AiProperties aiProperties;
    private final HttpClient http;

    public ExternalDataSourceService(SourceProperties properties, AiProperties aiProperties) {
        this.properties = properties;
        this.aiProperties = aiProperties;
        HttpClient.Builder builder = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(12))
            .followRedirects(HttpClient.Redirect.NORMAL);
        if (has(properties.outboundProxy())) {
            URI proxy = proxyUri(properties.outboundProxy());
            int port = proxy.getPort() > 0 ? proxy.getPort() : 80;
            builder.proxy(ProxySelector.of(new InetSocketAddress(proxy.getHost(), port)));
        }
        this.http = builder.build();
    }

    public List<DataSourceStatus> statuses() {
        return List.of(
            status("google-trends", "Google Trends 实时趋势", "signal", "RSS", properties.googleTrendsEnabled(), true, true,
                "日本、美国、东南亚实时搜索趋势信号，直接入库用于趋势雷达。",
                "https://trends.google.com/trending", "无需账号或 Key；默认同步 JP/US/SG。",
                List.of("无需材料"), List.of("GOOGLE_TRENDS_ENABLED", "GOOGLE_TRENDS_REGIONS", "OUTBOUND_HTTP_PROXY（可选）")),
            status("frankfurter", "Frankfurter 公共汇率", "rate", "public-api", properties.frankfurterEnabled(), true, true,
                "同步 JPY/CNY、USD/CNY 等央行参考汇率，用于多币种利润换算。",
                "https://frankfurter.dev/", "无需账号或 Key；汇率按日期缓存到 MySQL。",
                List.of("无需材料"), List.of("FRANKFURTER_ENABLED")),
            status("woocommerce", "WooCommerce 公共商品目录", "catalog", "store-api", wooConfigured(), true, false,
                "采集公开店铺的商品、价格、图片、类目和热销排序；默认接入日本商品跨境店。",
                "https://developer.woocommerce.com/docs/apis/store-api/resources-endpoints/products",
                "无需 Key；可把 WOOCOMMERCE_STORE_URLS 换成你的店铺或目标公开店铺。",
                List.of("WooCommerce 店铺首页 URL（默认已提供）"), List.of("WOOCOMMERCE_ENABLED", "WOOCOMMERCE_STORE_URLS")),
            status("yahoo-shopping", "Yahoo! Japan Shopping", "catalog", "official-api", has(properties.yahooShoppingClientId()), false, false,
                "日本商品搜索、含税价格、图片、评分与评论数，支持跨境代购筛选。",
                "https://developer.yahoo.co.jp/webapi/shopping/v3/itemsearch.html", "申请 Client ID 后即可参与真实日报。",
                List.of("Yahoo! JAPAN 开发者账号", "应用 Client ID"), List.of("YAHOO_SHOPPING_CLIENT_ID")),
            status("rakuten", "Rakuten Ichiba", "catalog", "official-api", rakutenConfigured(), false, false,
                "日本乐天商品、价格、图片、评论和海外配送信息。",
                "https://webservice.rakuten.co.jp/index.php/documentation/ichiba-item-search", "2026 版接口同时需要 Application ID 与 Access Key；Affiliate ID 可选。",
                List.of("Rakuten Web Service 应用", "Application ID", "Access Key", "Affiliate ID（可选）"),
                List.of("RAKUTEN_APPLICATION_ID", "RAKUTEN_ACCESS_KEY", "RAKUTEN_AFFILIATE_ID（可选）")),
            status("rainforest", "Amazon / Rainforest API", "catalog", value(properties.amazonMode(), "rainforest"), has(properties.rainforestApiKey()), false, false,
                "Amazon JP 搜索、商品价格、评分、排名和图片。",
                "https://www.rainforestapi.com/docs/product-data-api/overview", "适合快速获得结构化 Amazon 数据，按服务商套餐计费。",
                List.of("Rainforest API 账号", "API Key"), List.of("RAINFOREST_API_KEY")),
            status("keepa", "Amazon / Keepa", "history", "keepa", has(properties.keepaApiKey()), false, false,
                "Amazon 价格历史、BSR、类目和报价历史。",
                "https://keepa.com/#!api", "当前显示接入位，后续可用于历史曲线增强。",
                List.of("Keepa 订阅", "API Key"), List.of("KEEPA_API_KEY")),
            status("deepseek", "DeepSeek 智能标准化", "enrichment", value(aiProperties.model(), "deepseek-chat"), aiConfigured(), false, false,
                "把外文商品标题标准化为中文选品名、关键词、品类和有依据的推荐理由。",
                "https://api-docs.deepseek.com/", "仅在后台开启智能模式且配置 Key 时调用；失败会保留原始商品数据。",
                List.of("DeepSeek API Key"), List.of("AI_ENRICHMENT_ENABLED", "DEEPSEEK_API_KEY")),
            status("tiktok-apify", "TikTok / Apify", "social", value(properties.tiktokMode(), "demo"), has(properties.apifyToken()), false, false,
                "TikTok 日本热视频与商品趋势采集。",
                "https://apify.com/clockworks/tiktok-scraper", "已保留配置位，当前日报适配器优先使用可验证商品目录。",
                List.of("Apify 账号", "API Token"), List.of("APIFY_TOKEN")),
            status("supplier-search", "1688 / 国内采购", "supplier", value(properties.supplierMode(), "search-link"), false, false, false,
                "生成 1688、淘宝、拼多多采购检索入口并估算成本。",
                "https://open.1688.com/", "当前价格为估算值并明确标注；真实报价需开放平台或供应商报价单。",
                List.of("如需真实报价：1688 开放平台应用或供应商报价表"), List.of("SUPPLIER_MODE"))
        );
    }

    public String get(String url) {
        return request("GET", url, null, Map.of("Accept", "application/json, application/xml, text/xml;q=0.9, */*;q=0.8"));
    }

    public String get(String url, Map<String, String> headers) {
        return request("GET", url, null, headers);
    }

    public String postJson(String url, String body, Map<String, String> headers) {
        Map<String, String> merged = new LinkedHashMap<>(headers);
        merged.putIfAbsent("Content-Type", "application/json");
        merged.putIfAbsent("Accept", "application/json");
        return request("POST", url, body, merged);
    }

    public Optional<String> tryGet(String url) {
        try {
            return Optional.of(get(url));
        } catch (DataSourceAccessException exception) {
            return Optional.empty();
        }
    }

    public String googleTrendsUrl(String region) {
        return "https://trends.google.com/trending/rss?geo=" + encode(region.toUpperCase(Locale.ROOT));
    }

    public String frankfurterRateUrl(String base, String quote) {
        return "https://api.frankfurter.dev/v2/rate/" + encode(base.toUpperCase(Locale.ROOT)) + "/" + encode(quote.toUpperCase(Locale.ROOT));
    }

    public Optional<String> rainforestSearchUrl(String query) {
        if (!has(properties.rainforestApiKey())) return Optional.empty();
        return Optional.of("https://api.rainforestapi.com/request?api_key=" + encode(properties.rainforestApiKey())
            + "&type=search&amazon_domain=amazon.co.jp&search_term=" + encode(query));
    }

    public Optional<String> rakutenSearchUrl(String query, int hits) {
        if (!rakutenConfigured()) return Optional.empty();
        String version = value(properties.rakutenApiVersion(), "20260701").replaceAll("[^0-9]", "");
        String affiliate = has(properties.rakutenAffiliateId())
            ? "&affiliateId=" + encode(properties.rakutenAffiliateId())
            : "";
        return Optional.of("https://openapi.rakuten.co.jp/ichibams/api/IchibaItem/Search/" + version
            + "?format=json&formatVersion=2&applicationId=" + encode(properties.rakutenApplicationId())
            + affiliate + "&keyword=" + encode(query)
            + "&hits=" + Math.min(Math.max(hits, 1), 30) + "&sort=" + encode("-reviewCount") + "&imageFlag=1&availability=1");
    }

    public Map<String, String> rakutenHeaders() {
        if (!rakutenConfigured()) return Map.of();
        return Map.of("Accept", "application/json", "accessKey", properties.rakutenAccessKey());
    }

    public Optional<String> yahooShoppingSearchUrl(String query, int results) {
        if (!has(properties.yahooShoppingClientId())) return Optional.empty();
        return Optional.of("https://shopping.yahooapis.jp/ShoppingWebService/V3/itemSearch?appid="
            + encode(properties.yahooShoppingClientId()) + "&query=" + encode(query) + "&results="
            + Math.min(Math.max(results, 1), 50) + "&sort=" + encode("-review_count")
            + "&in_stock=true&condition=new&is_cross_border_agency=true&image_size=300");
    }

    public String woocommerceProductsUrl(String baseUrl, int limit) {
        String base = baseUrl == null ? "" : baseUrl.trim().replaceAll("/+$", "");
        URI uri = URI.create(base);
        if (!List.of("http", "https").contains(uri.getScheme()) || uri.getHost() == null) {
            throw new DataSourceAccessException("WooCommerce 店铺 URL 不合法");
        }
        return base + "/wp-json/wc/store/v1/products?per_page=" + Math.min(Math.max(limit, 1), 100)
            + "&orderby=popularity&order=desc";
    }

    public List<String> woocommerceStores() {
        if (!properties.woocommerceEnabled()) return List.of();
        return split(properties.woocommerceStoreUrls());
    }

    public List<String> googleTrendRegions() {
        return split(properties.googleTrendsRegions()).stream().map(value -> value.toUpperCase(Locale.ROOT)).toList();
    }

    public boolean googleTrendsEnabled() { return properties.googleTrendsEnabled(); }
    public boolean frankfurterEnabled() { return properties.frankfurterEnabled(); }
    public boolean rakutenConfigured() { return has(properties.rakutenApplicationId()) && has(properties.rakutenAccessKey()); }
    public boolean yahooConfigured() { return has(properties.yahooShoppingClientId()); }
    public boolean rainforestConfigured() { return has(properties.rainforestApiKey()); }
    public boolean aiConfigured() { return aiProperties.enabled() && has(aiProperties.apiKey()); }

    private String request(String method, String url, String body, Map<String, String> headers) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(30))
                .header("User-Agent", USER_AGENT);
            headers.forEach(builder::header);
            builder.method(method, body == null
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));
            HttpResponse<String> response = http.send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new DataSourceAccessException("上游接口返回 HTTP " + response.statusCode(), response.statusCode());
            }
            return response.body();
        } catch (DataSourceAccessException exception) {
            throw exception;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new DataSourceAccessException("访问上游数据源失败：" + exception.getMessage(), exception);
        } catch (Exception exception) {
            throw new DataSourceAccessException("访问上游数据源失败：" + exception.getMessage(), exception);
        }
    }

    private DataSourceStatus status(
        String key, String name, String type, String mode, boolean configured, boolean live, boolean supportsCollect,
        String useCase, String docsUrl, String note, List<String> materials, List<String> env
    ) {
        boolean supportsTest = Set.of(
            "google-trends", "frankfurter", "woocommerce", "yahoo-shopping", "rakuten", "rainforest", "deepseek"
        ).contains(key);
        return new DataSourceStatus(
            key, name, type, mode, configured, live, supportsTest, supportsCollect, useCase, docsUrl, note, materials, env
        );
    }

    private boolean wooConfigured() {
        return properties.woocommerceEnabled() && !woocommerceStores().isEmpty();
    }

    private boolean has(String value) {
        return StringUtils.hasText(value);
    }

    private String value(String input, String fallback) {
        return has(input) ? input.trim() : fallback;
    }

    private String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private URI proxyUri(String value) {
        try {
            String normalized = value.trim().contains("://") ? value.trim() : "http://" + value.trim();
            URI proxy = URI.create(normalized);
            if (proxy.getHost() == null || proxy.getPort() == 0) {
                throw new IllegalArgumentException("缺少主机或端口");
            }
            return proxy;
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("OUTBOUND_HTTP_PROXY 格式不正确，应类似 http://127.0.0.1:20808", exception);
        }
    }

    private List<String> split(String value) {
        if (!has(value)) return List.of();
        return Arrays.stream(value.split("[,\n]"))
            .map(String::trim)
            .filter(item -> !item.isBlank())
            .distinct()
            .toList();
    }
}
