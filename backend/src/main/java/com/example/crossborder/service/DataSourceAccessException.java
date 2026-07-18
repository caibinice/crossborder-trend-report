package com.example.crossborder.service;

public class DataSourceAccessException extends RuntimeException {
    public DataSourceAccessException(String message) {
        super(message);
    }

    public DataSourceAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
