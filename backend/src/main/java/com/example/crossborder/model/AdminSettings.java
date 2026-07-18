package com.example.crossborder.model;
import java.math.BigDecimal;
import java.util.List;
public record AdminSettings(
    List<String> foreignSources,
    List<String> domesticSources,
    List<String> categories,
    List<String> regions,
    String sourceMode,
    String frequencyCron,
    int maxProducts,
    BigDecimal jpyCnyRate,
    boolean autoExchangeRate,
    BigDecimal defaultShippingCny,
    boolean smartMode
) {}
