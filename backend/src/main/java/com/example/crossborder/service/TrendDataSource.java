package com.example.crossborder.service;

import com.example.crossborder.model.AdminSettings;
import com.example.crossborder.model.TrendCandidate;
import java.time.LocalDate;
import java.util.List;

public interface TrendDataSource {
    List<TrendCandidate> fetch(LocalDate date, AdminSettings settings);
}
