package com.example.crossborder.model;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
public record TrendReport(long id,LocalDate reportDate,String sourceMode,String title,String summary,Instant createdAt,List<TrendProduct> products) {}
