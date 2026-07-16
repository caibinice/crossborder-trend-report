package com.example.crossborder.controller;

import java.time.Instant;

public record ApiError(String code, String message, int status, String path, Instant timestamp) {}
