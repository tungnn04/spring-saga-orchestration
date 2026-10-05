package com.example.orderservice.exception;

public class SagaExecutionException extends RuntimeException {
    public SagaExecutionException(String message, Throwable cause) {
        super(message, cause);
    }
    public SagaExecutionException(String message) {
        super(message);
    }
}
