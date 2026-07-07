package com.example.crossborder.model;
import java.math.BigDecimal;
public record ScheduleConfig(String frequencyCron,int maxProducts,BigDecimal jpyCnyRate,BigDecimal defaultShippingCny,boolean smartMode) {}
