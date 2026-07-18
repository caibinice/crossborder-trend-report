package com.example.crossborder.service;

import com.example.crossborder.model.CollectionRun;
import com.example.crossborder.model.SourceTestResult;
import com.example.crossborder.model.TrendSignal;
import com.example.crossborder.repository.DataIngestionRepository;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilderFactory;
import org.springframework.stereotype.Service;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

@Service
public class GoogleTrendsService {
    public static final String SOURCE_KEY = "google-trends";
    private static final Pattern TRAFFIC = Pattern.compile("([0-9]+(?:\\.[0-9]+)?)\\s*([KMB]?)", Pattern.CASE_INSENSITIVE);

    private final ExternalDataSourceService external;
    private final DataIngestionRepository repository;

    public GoogleTrendsService(ExternalDataSourceService external, DataIngestionRepository repository) {
        this.external = external;
        this.repository = repository;
    }

    public CollectionRun collect(String region, String triggerType) {
        String normalizedRegion = normalizeRegion(region);
        if (!external.googleTrendsEnabled()) {
            throw new ApiValidationException("Google Trends 数据源已关闭");
        }
        long runId = repository.startRun(SOURCE_KEY, triggerType);
        try {
            List<TrendSignal> signals = fetch(normalizedRegion);
            repository.saveSignals(signals);
            return repository.completeRun(runId, signals.size(), "已同步 " + normalizedRegion + " 实时趋势 " + signals.size() + " 条");
        } catch (RuntimeException exception) {
            repository.failRun(runId, rootMessage(exception));
            throw exception;
        }
    }

    public SourceTestResult test(String region) {
        String normalizedRegion = normalizeRegion(region);
        try {
            List<TrendSignal> signals = fetch(normalizedRegion);
            return new SourceTestResult(SOURCE_KEY, true, signals.size(), "连接成功，读取到 " + signals.size() + " 条趋势", Instant.now());
        } catch (RuntimeException exception) {
            return new SourceTestResult(SOURCE_KEY, false, 0, rootMessage(exception), Instant.now());
        }
    }

    public List<TrendSignal> list(String region, int limit) {
        return repository.signals(region, limit);
    }

    public int count() {
        return repository.countSignals();
    }

    List<TrendSignal> fetch(String region) {
        String xml = external.get(
            external.googleTrendsUrl(region),
            java.util.Map.of("Accept", "application/rss+xml, application/xml;q=0.9, text/xml;q=0.8")
        );
        return parse(xml, region);
    }

    List<TrendSignal> parse(String xml, String region) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            NodeList items = factory.newDocumentBuilder()
                .parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)))
                .getElementsByTagName("item");
            List<TrendSignal> signals = new ArrayList<>();
            Instant fetchedAt = Instant.now();
            for (int index = 0; index < items.getLength(); index++) {
                Element item = (Element) items.item(index);
                String keyword = child(item, "title");
                if (keyword.isBlank()) continue;
                String trafficLabel = namespaced(item, "approx_traffic");
                signals.add(new TrendSignal(
                    0, SOURCE_KEY, region, keyword, trafficLabel, trafficValue(trafficLabel), child(item, "link"),
                    namespaced(item, "picture"), publishedAt(child(item, "pubDate"), fetchedAt), fetchedAt
                ));
            }
            if (signals.isEmpty()) {
                throw new DataSourceAccessException("Google Trends 返回了空趋势列表");
            }
            return signals;
        } catch (DataSourceAccessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new DataSourceAccessException("Google Trends RSS 解析失败：" + exception.getMessage(), exception);
        }
    }

    long trafficValue(String label) {
        String value = label == null ? "" : label.replace(",", "").trim().toUpperCase(Locale.ROOT);
        Matcher matcher = TRAFFIC.matcher(value);
        if (!matcher.find()) return 0;
        double number = Double.parseDouble(matcher.group(1));
        double multiplier = switch (matcher.group(2)) {
            case "K" -> 1_000D;
            case "M" -> 1_000_000D;
            case "B" -> 1_000_000_000D;
            default -> 1D;
        };
        return Math.max(0L, Math.round(number * multiplier));
    }

    private String normalizeRegion(String region) {
        String value = region == null || region.isBlank() ? "JP" : region.trim().toUpperCase(Locale.ROOT);
        if (!value.matches("[A-Z]{2}")) {
            throw new ApiValidationException("趋势区域必须是两位国家或地区代码");
        }
        List<String> allowed = external.googleTrendRegions();
        if (!allowed.isEmpty() && !allowed.contains(value)) {
            throw new ApiValidationException("趋势区域未启用，可用区域：" + String.join(", ", allowed));
        }
        return value;
    }

    private String child(Element parent, String tag) {
        NodeList nodes = parent.getElementsByTagName(tag);
        return nodes.getLength() == 0 ? "" : nodes.item(0).getTextContent().trim();
    }

    private String namespaced(Element parent, String localName) {
        NodeList nodes = parent.getElementsByTagNameNS("*", localName);
        return nodes.getLength() == 0 ? "" : nodes.item(0).getTextContent().trim();
    }

    private Instant publishedAt(String value, Instant fallback) {
        try {
            return ZonedDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant();
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) current = current.getCause();
        return current.getMessage() == null ? throwable.getClass().getSimpleName() : current.getMessage();
    }
}
