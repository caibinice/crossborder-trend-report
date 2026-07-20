package com.example.crossborder.service;

public class DataSourceAccessException extends RuntimeException {
    private final Integer statusCode;

    public DataSourceAccessException(String message) {
        this(message, null, null);
    }

    public DataSourceAccessException(String message, Throwable cause) {
        this(message, null, cause);
    }

    public DataSourceAccessException(String message, int statusCode) {
        this(message, statusCode, null);
    }

    private DataSourceAccessException(String message, Integer statusCode, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
    }

    public boolean hasStatus(int expectedStatus) {
        return statusCode != null && statusCode == expectedStatus;
    }
}
