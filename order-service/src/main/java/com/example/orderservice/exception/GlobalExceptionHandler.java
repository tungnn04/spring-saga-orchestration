package com.example.orderservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(SagaExecutionException.class)
    public ResponseEntity<Map<String, Object>> handleSagaException(SagaExecutionException e) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(Map.of(
                "timestamp", LocalDateTime.now().toString(),
                "error", "Saga Execution Failed",
                "message", e.getMessage()
        ));
    }

    @ExceptionHandler(CompensationFailedException.class)
    public ResponseEntity<Map<String, Object>> handleCompensationException(CompensationFailedException e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "timestamp", LocalDateTime.now().toString(),
                "error", "Compensation Failed",
                "message", e.getMessage()
        ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "timestamp", LocalDateTime.now().toString(),
                "error", "Internal Server Error",
                "message", e.getMessage()
        ));
    }
}
