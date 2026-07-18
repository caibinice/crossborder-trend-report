package com.example.crossborder.model;

import java.time.Instant;

public record TrendSignal(
    long id,
    String sourceKey,
    String region,
    String keyword,
    String trafficLabel,
    long trafficValue,
    String sourceUrl,
    String imageUrl,
    Instant publishedAt,
    Instant fetchedAt
) {}
