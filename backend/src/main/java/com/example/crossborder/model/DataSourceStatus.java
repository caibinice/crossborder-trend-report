package com.example.crossborder.model;

import java.util.List;

public record DataSourceStatus(
    String key,
    String name,
    String type,
    String mode,
    boolean configured,
    boolean live,
    boolean supportsTest,
    boolean supportsCollect,
    String useCase,
    String docsUrl,
    String note,
    List<String> requiredMaterials,
    List<String> environmentVariables
) {}
