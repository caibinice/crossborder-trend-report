package com.example.crossborder.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.crossborder.model.TrendCandidate;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class CandidateRankingServiceTest {
    @Test
    void normalizesVolumeAmountAndCompositeHeatToOneThroughOneHundred() {
        List<TrendCandidate> ranked = new CandidateRankingService().rank(List.of(
            candidate("A", 100, 1000, 90),
            candidate("B", 10, 500, 60),
            candidate("C", 1, 10, 30)
        ));

        assertEquals(100D, ranked.get(0).salesVolumeScore());
        assertEquals(1D, ranked.get(2).salesVolumeScore());
        assertEquals(100D, ranked.get(0).heatScore());
        assertEquals(1D, ranked.get(2).heatScore());
        assertTrue(ranked.stream().allMatch(item -> item.heatScore() >= 1 && item.heatScore() <= 100));
    }

    private TrendCandidate candidate(String name, double volume, double amount, double ai) {
        return new TrendCandidate(
            "玩具", name, name, name, "Rakuten Ichiba", "https://example.com/" + name, null,
            volume, volume, amount, ai, BigDecimal.TEN, "JPY", "测试"
        );
    }
}
