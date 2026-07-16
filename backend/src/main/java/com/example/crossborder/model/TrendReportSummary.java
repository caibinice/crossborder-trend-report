package com.example.crossborder.model;

import java.time.Instant;
import java.time.LocalDate;

public record TrendReportSummary(
    long id,
    LocalDate reportDate,
    String sourceMode,
    String title,
    String summary,
    Instant createdAt,
    int productCount
) {}
