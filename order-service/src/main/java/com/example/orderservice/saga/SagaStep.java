package com.example.orderservice.saga;

import com.example.orderservice.exception.CompensationFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;
import java.util.function.Supplier;

public record SagaStep<T>(String name, Supplier<T> action, Consumer<T> compensation, int maxRetries) {

    private static final Logger log = LoggerFactory.getLogger(SagaStep.class);

    public T execute() {
        log.info("[SAGA] Executing step: {}", name);
        return action.get();
    }

    public void compensate(T result) {
        int attempts = 0;
        while (attempts < maxRetries) {
            try {
                log.info("[SAGA] Compensating step: {} (attempt {})", name, attempts + 1);
                compensation.accept(result);
                log.info("[SAGA] Compensation successful for step: {}", name);
                return;
            } catch (Exception e) {
                attempts++;
                log.warn("[SAGA] Compensation attempt {} failed for step: {}", attempts, name, e);
                if (attempts >= maxRetries) {
                    throw new CompensationFailedException(
                            "Failed to compensate step after " + maxRetries + " attempts: " + name, e);
                }
                // Exponential backoff
                sleep(1000L * (long) Math.pow(2, attempts));
            }
        }
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CompensationFailedException(
                    "Interrupted while compensating step: " + name, e);
        }
    }
}
