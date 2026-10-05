package com.example.orderservice.exception;

public class CompensationFailedException extends RuntimeException {
    public CompensationFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
