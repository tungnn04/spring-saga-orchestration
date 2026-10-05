package com.example.paymentservice.service;

import com.example.paymentservice.domain.Payment;
import com.example.paymentservice.domain.PaymentStatus;
import com.example.paymentservice.dto.PaymentRequest;
import com.example.paymentservice.dto.PaymentResponse;
import com.example.paymentservice.repository.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRepository paymentRepository;

    public PaymentService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    @Transactional
    public PaymentResponse processPayment(PaymentRequest request) {
        log.info("[PAYMENT] Processing payment for orderId: {}, idempotencyKey: {}",
                request.getOrderId(), request.getIdempotencyKey());

        // Idempotency check - exactly as shown in the demo code
        if (request.getIdempotencyKey() != null) {
            var existing = paymentRepository.findByIdempotencyKey(request.getIdempotencyKey());
            if (existing.isPresent()) {
                log.info("[PAYMENT] Duplicate payment request detected, returning existing payment");
                return mapToResponse(existing.get());
            }
        }

        // Simulate payment processing (in real world: call payment gateway)
        // For demo: reject payments with amount > 10000
        if (request.getAmount() != null && request.getAmount().compareTo(new java.math.BigDecimal("10000")) > 0) {
            throw new IllegalStateException("Payment amount exceeds limit: " + request.getAmount());
        }

        Payment payment = Payment.builder()
                .orderId(request.getOrderId())
                .customerId(request.getCustomerId())
                .amount(request.getAmount())
                .status(PaymentStatus.COMPLETED)
                .idempotencyKey(request.getIdempotencyKey() != null
                        ? request.getIdempotencyKey()
                        : UUID.randomUUID().toString())
                .build();

        payment = paymentRepository.save(payment);
        log.info("[PAYMENT] Payment processed successfully: {}", payment.getId());
        return mapToResponse(payment);
    }

    @Transactional
    public PaymentResponse refundPayment(UUID paymentId) {
        log.info("[PAYMENT] Refunding payment: {}", paymentId);
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + paymentId));

        payment.setStatus(PaymentStatus.REFUNDED);
        payment = paymentRepository.save(payment);
        log.info("[PAYMENT] Payment refunded: {}", paymentId);
        return mapToResponse(payment);
    }

    public PaymentResponse getPayment(UUID paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + paymentId));
        return mapToResponse(payment);
    }

    private PaymentResponse mapToResponse(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .orderId(payment.getOrderId())
                .customerId(payment.getCustomerId())
                .amount(payment.getAmount())
                .status(payment.getStatus())
                .idempotencyKey(payment.getIdempotencyKey())
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .build();
    }
}
