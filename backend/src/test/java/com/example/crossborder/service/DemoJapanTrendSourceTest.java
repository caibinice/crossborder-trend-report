package com.example.crossborder.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class DemoJapanTrendSourceTest {
    @Test
    void fetch_returnsOnlyUniqueCandidatesWithoutPlaceholderNames() {
        var candidates = new DemoJapanTrendSource().fetch(LocalDate.of(2026, 7, 11));

        assertEquals(21, candidates.size());
        assertEquals(candidates.size(), candidates.stream().map(candidate -> candidate.productNameCn()).distinct().count());
        assertTrue(candidates.stream().noneMatch(candidate -> candidate.productNameCn().contains("???")));
    }
}
