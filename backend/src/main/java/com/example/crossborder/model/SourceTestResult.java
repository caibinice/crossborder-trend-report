package com.example.crossborder.model;

import java.time.Instant;

public record SourceTestResult(
    String sourceKey,
    boolean success,
    int itemCount,
    String message,
    Instant checkedAt
) {}
