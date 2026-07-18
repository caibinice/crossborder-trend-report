package com.example.crossborder.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record ExchangeRateSnapshot(
    long id,
    String baseCurrency,
    String quoteCurrency,
    LocalDate rateDate,
    BigDecimal rateValue,
    String provider,
    Instant fetchedAt
) {}
