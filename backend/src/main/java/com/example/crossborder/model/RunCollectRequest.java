package com.example.crossborder.model;
import java.time.LocalDate;
public record RunCollectRequest(LocalDate reportDate, Boolean force, String marketKey) {
    public RunCollectRequest(LocalDate reportDate, Boolean force) {
        this(reportDate, force, "jp");
    }
}
