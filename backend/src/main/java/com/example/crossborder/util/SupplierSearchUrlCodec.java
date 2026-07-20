package com.example.crossborder.util;

import com.example.crossborder.model.DomesticLink;
import com.example.crossborder.model.SupplierSiteConfig;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public final class SupplierSearchUrlCodec {
    private static final Charset GB18030 = Charset.forName("GB18030");

    private SupplierSearchUrlCodec() {}

    public static String encodeKeyword(SupplierSiteConfig site, String query) {
        return URLEncoder.encode(query, usesGb18030(site.name(), site.urlTemplate()) ? GB18030 : StandardCharsets.UTF_8);
    }

    public static String encodingLabel(SupplierSiteConfig site) {
        return usesGb18030(site.name(), site.urlTemplate()) ? "GB18030/GBK" : "UTF-8";
    }

    /**
     * Older reports stored 1688 keywords as UTF-8. 1688's current search form still decodes this
     * parameter as GBK-compatible bytes, so normalize those links while reading historical data.
     */
    public static DomesticLink normalizeHistorical(DomesticLink link) {
        if (!usesGb18030(link.platform(), link.url())) return link;
        String query = queryFromTitle(link.title(), link.platform());
        if (query.isBlank()) return link;
        String url = replaceParameter(link.url(), "keywords", URLEncoder.encode(query, GB18030));
        String note = link.note() == null ? null : link.note().replace("UTF-8", "GB18030/GBK");
        if (url.equals(link.url()) && java.util.Objects.equals(note, link.note())) return link;
        return new DomesticLink(
            link.id(), link.productId(), link.platform(), link.title(), url, link.priceCny(), note
        );
    }

    private static boolean usesGb18030(String name, String url) {
        String value = ((name == null ? "" : name) + " " + (url == null ? "" : url)).toLowerCase(Locale.ROOT);
        return value.contains("1688") || value.contains("s.1688.com");
    }

    private static String queryFromTitle(String title, String platform) {
        if (title == null || title.isBlank()) return "";
        String marker = " - " + (platform == null ? "1688" : platform) + "搜索";
        if (title.endsWith(marker)) return title.substring(0, title.length() - marker.length()).trim();
        int fallback = title.lastIndexOf(" - 1688搜索");
        return fallback > 0 ? title.substring(0, fallback).trim() : "";
    }

    private static String replaceParameter(String url, String parameter, String encodedValue) {
        if (url == null || url.isBlank()) return url;
        String token = parameter.toLowerCase(Locale.ROOT) + "=";
        int tokenStart = url.toLowerCase(Locale.ROOT).indexOf(token);
        if (tokenStart < 0) return url;
        int valueStart = tokenStart + token.length();
        int valueEnd = url.length();
        int ampersand = url.indexOf('&', valueStart);
        int fragment = url.indexOf('#', valueStart);
        if (ampersand >= 0) valueEnd = Math.min(valueEnd, ampersand);
        if (fragment >= 0) valueEnd = Math.min(valueEnd, fragment);
        return url.substring(0, valueStart) + encodedValue + url.substring(valueEnd);
    }
}
