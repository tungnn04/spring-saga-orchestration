package com.example.orderservice.saga;

import com.example.orderservice.dto.SagaResult;
import com.example.orderservice.exception.CompensationFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class SagaExecutor {

    private static final Logger log = LoggerFactory.getLogger(SagaExecutor.class);

    private final List<SagaStep<?>> steps = new ArrayList<>();
    private final List<Object> completedResults = new ArrayList<>();
    private final String sagaId;

    public SagaExecutor(String sagaId) {
        this.sagaId = sagaId;
    }

    public <T> void addStep(SagaStep<T> step) {
        steps.add(step);
    }

    @SuppressWarnings("unchecked")
    public SagaResult run() {
        log.info("[SAGA {}] Starting saga execution with {} steps", sagaId, steps.size());

        for (int i = 0; i < steps.size(); i++) {
            SagaStep<Object> step = (SagaStep<Object>) steps.get(i);
            try {
                Object result = step.execute();
                completedResults.add(result);
                log.info("[SAGA {}] Step '{}' completed successfully", sagaId, step.name());
            } catch (Exception e) {
                log.error("[SAGA {}] Step '{}' failed: {}", sagaId, step.name(), e.getMessage());
                String failedStep = step.name();
                compensateCompleted(i - 1);
                return SagaResult.builder()
                        .success(false)
                        .message("Saga failed at step: " + failedStep + " - " + e.getMessage())
                        .failedStep(failedStep)
                        .build();
            }
        }

        log.info("[SAGA {}] Saga completed successfully", sagaId);
        return SagaResult.builder()
                .success(true)
                .message("Saga completed successfully")
                .build();
    }

    @SuppressWarnings("unchecked")
    private void compensateCompleted(int fromIndex) {
        log.info("[SAGA {}] Starting compensation from step index {}", sagaId, fromIndex);
        for (int i = fromIndex; i >= 0; i--) {
            SagaStep<Object> step = (SagaStep<Object>) steps.get(i);
            Object result = completedResults.get(i);
            try {
                step.compensate(result);
            } catch (CompensationFailedException e) {
                log.error("[SAGA {}] CRITICAL: Compensation failed for step '{}': {}",
                        sagaId, step.name(), e.getMessage());
                // Continue compensating other steps even if one fails
            }
        }
    }
}
