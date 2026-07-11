package com.example.crossborder.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security")
public record SecurityProperties(boolean enabled, String jwtSecret, long jwtExpireMinutes) {}
