package com.example.crossborder.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.crossborder.config.AiProperties;
import com.example.crossborder.config.SourceProperties;
import com.example.crossborder.model.DataSourceStatus;
import java.util.List;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class ExternalDataSourceServiceTest {
    private static final Pattern HAN = Pattern.compile("[\\p{IsHan}]");
    private final ExternalDataSourceService service = new ExternalDataSourceService(
        new SourceProperties(
            true, "JP,US,SG", true, true,
            "https://jp.example", "https://us.example", "https://sea.example",
            "demo", "", "", "", "", "rainforest", "", "", "",
            "", "", "", "", "", "", "search-link", ""
        ),
        new AiProperties(true, "https://api.example", "test-key", "test-model", true, "high", 30)
    );

    @Test
    void keepsJapaneseMarketStatusCopyInJapanese() {
        assertTrue(HAN.matcher(flatten(service.statuses("jp"))).find());
    }

    @Test
    void exposesChineseStatusCopyForUnitedStatesAndSoutheastAsia() {
        assertTrue(HAN.matcher(flatten(service.statuses("us"))).find());
        assertTrue(HAN.matcher(flatten(service.statuses("sea"))).find());
        assertFalse(flatten(service.statuses("us")).contains("public catalogs"));
        assertFalse(flatten(service.statuses("sea")).contains("China sourcing"));
    }

    private String flatten(List<DataSourceStatus> statuses) {
        return statuses.stream()
            .map(status -> String.join(" ",
                status.name(),
                status.useCase(),
                status.note(),
                String.join(" ", status.requiredMaterials()),
                String.join(" ", status.environmentVariables())
            ))
            .reduce("", (left, right) -> left + " " + right);
    }
}
