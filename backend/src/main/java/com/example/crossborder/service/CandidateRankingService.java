package com.example.crossborder.service;

import com.example.crossborder.model.TrendCandidate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.ToDoubleFunction;
import org.springframework.stereotype.Service;

@Service
public class CandidateRankingService {
    public List<TrendCandidate> rank(List<TrendCandidate> candidates) {
        if (candidates.isEmpty()) return candidates;
        double[] volume = groupedPercentiles(candidates, TrendCandidate::salesVolumeScore);
        double[] amount = groupedPercentiles(candidates, TrendCandidate::salesAmountScore);
        double[] composite = new double[candidates.size()];
        for (int index = 0; index < candidates.size(); index++) {
            double ai = clamp(candidates.get(index).aiScore(), 1D, 100D);
            composite[index] = volume[index] * 0.45D + amount[index] * 0.30D + ai * 0.25D;
        }
        double[] heat = globalPercentiles(candidates, composite, volume, amount);
        List<TrendCandidate> ranked = new ArrayList<>(candidates.size());
        for (int index = 0; index < candidates.size(); index++) {
            TrendCandidate item = candidates.get(index);
            ranked.add(new TrendCandidate(
                item.category(), item.productNameJp(), item.productNameCn(), item.keywords(), item.sourcePlatform(),
                item.sourceUrl(), item.imageUrl(), round(heat[index]), round(volume[index]), round(amount[index]),
                round(clamp(item.aiScore(), 1D, 100D)), item.sourcePrice(), item.sourceCurrency(), item.reason()
            ));
        }
        return List.copyOf(ranked);
    }

    private double[] groupedPercentiles(List<TrendCandidate> candidates, ToDoubleFunction<TrendCandidate> extractor) {
        Map<String, List<Integer>> groups = new LinkedHashMap<>();
        for (int index = 0; index < candidates.size(); index++) {
            TrendCandidate item = candidates.get(index);
            String key = item.sourcePlatform() + "\u0000" + item.category();
            groups.computeIfAbsent(key, ignored -> new ArrayList<>()).add(index);
        }
        double[] scores = new double[candidates.size()];
        for (List<Integer> indices : groups.values()) {
            indices.sort(Comparator
                .comparingDouble((Integer index) -> safe(extractor.applyAsDouble(candidates.get(index)))).reversed()
                .thenComparing(Comparator.comparingDouble((Integer index) -> candidates.get(index).heatScore()).reversed())
                .thenComparing(index -> candidates.get(index).productNameJp()));
            assignPercentiles(indices, scores);
        }
        return scores;
    }

    private double[] globalPercentiles(
        List<TrendCandidate> candidates, double[] composite, double[] volume, double[] amount
    ) {
        List<Integer> indices = new ArrayList<>();
        for (int index = 0; index < candidates.size(); index++) indices.add(index);
        indices.sort(Comparator
            .comparingDouble((Integer index) -> composite[index]).reversed()
            .thenComparing(Comparator.comparingDouble((Integer index) -> amount[index]).reversed())
            .thenComparing(Comparator.comparingDouble((Integer index) -> volume[index]).reversed())
            .thenComparing(Comparator.comparingDouble((Integer index) -> candidates.get(index).heatScore()).reversed())
            .thenComparing(index -> candidates.get(index).productNameJp()));
        double[] scores = new double[candidates.size()];
        assignPercentiles(indices, scores);
        return scores;
    }

    private void assignPercentiles(List<Integer> indices, double[] output) {
        if (indices.size() == 1) {
            output[indices.get(0)] = 100D;
            return;
        }
        for (int position = 0; position < indices.size(); position++) {
            output[indices.get(position)] = 100D - 99D * position / (indices.size() - 1D);
        }
    }

    private double safe(double value) {
        return Double.isFinite(value) ? Math.max(0D, value) : 0D;
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, Double.isFinite(value) ? value : min));
    }

    private double round(double value) {
        return Math.round(value * 10D) / 10D;
    }
}
