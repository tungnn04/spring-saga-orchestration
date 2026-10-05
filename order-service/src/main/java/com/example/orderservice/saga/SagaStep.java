package com.example.orderservice.saga;

import com.example.orderservice.exception.CompensationFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class SagaStep<T> {

    private static final Logger log = LoggerFactory.getLogger(SagaStep.class);

    private final String name;
    private final Supplier<T> action;
    private final Consumer<T> compensation;
    private final int maxRetries;

    public SagaStep(String name, Supplier<T> action, Consumer<T> compensation, int maxRetries) {
        this.name = name;
        this.action = action;
        this.compensation = compensation;
        this.maxRetries = maxRetries;
    }

    public String getName() {
        return name;
    }

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
