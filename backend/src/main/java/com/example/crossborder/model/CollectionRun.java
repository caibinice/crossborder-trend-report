package com.example.crossborder.model;

import java.time.Instant;

public record CollectionRun(
    long id,
    String sourceKey,
    String triggerType,
    String status,
    int itemCount,
    String message,
    Instant startedAt,
    Instant finishedAt
) {}
