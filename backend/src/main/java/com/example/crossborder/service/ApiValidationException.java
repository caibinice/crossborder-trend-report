package com.example.crossborder.service;

public class ApiValidationException extends RuntimeException {
    public ApiValidationException(String message) {
        super(message);
    }
}
