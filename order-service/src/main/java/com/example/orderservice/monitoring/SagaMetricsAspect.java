package com.example.orderservice.monitoring;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class SagaMetricsAspect {

    private final MeterRegistry meterRegistry;

    public SagaMetricsAspect(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Around("@annotation(SagaStepMetric)")
    public Object measureStep(ProceedingJoinPoint joinPoint) throws Throwable {
        String stepName = joinPoint.getSignature().getName();
        Timer.Sample sample = Timer.start(meterRegistry);

        try {
            Object result = joinPoint.proceed();
            meterRegistry.counter("saga.step.success", "step", stepName).increment();
            return result;
        } catch (Exception e) {
            meterRegistry.counter("saga.step.failure", "step", stepName).increment();
            throw e;
        } finally {
            sample.stop(meterRegistry.timer("saga.step.duration", "step", stepName));
        }
    }
}
