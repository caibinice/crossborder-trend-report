package com.example.crossborder.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import com.example.crossborder.model.TrendSignal;
import com.example.crossborder.repository.DataIngestionRepository;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class GoogleTrendsServiceTest {
    private final GoogleTrendsService service = new GoogleTrendsService(
        mock(ExternalDataSourceService.class), mock(DataIngestionRepository.class)
    );

    @Test
    void parsesNamespacedRssAndTraffic() {
        String xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <rss xmlns:ht="https://trends.google.com/trending/rss" version="2.0"><channel>
              <item>
                <title>新商品 トレンド</title>
                <ht:approx_traffic>20K+</ht:approx_traffic>
                <link>https://example.com/trend</link>
                <ht:picture>https://example.com/image.jpg</ht:picture>
                <pubDate>Fri, 17 Jul 2026 02:00:00 GMT</pubDate>
              </item>
            </channel></rss>
            """;

        List<TrendSignal> signals = service.parse(xml, "JP");

        assertEquals(1, signals.size());
        assertEquals("新商品 トレンド", signals.get(0).keyword());
        assertEquals(20_000L, signals.get(0).trafficValue());
        assertEquals("JP", signals.get(0).region());
        assertEquals(Instant.parse("2026-07-17T02:00:00Z"), signals.get(0).publishedAt());
    }

    @Test
    void parsesTrafficUnitsAndRejectsEmptyFeed() {
        assertEquals(200L, service.trafficValue("200+"));
        assertEquals(1_500_000L, service.trafficValue("1.5M+"));
        assertThrows(DataSourceAccessException.class, () -> service.parse("<rss><channel/></rss>", "JP"));
    }
}
