package com.example.paymentservice.messaging;

import com.example.paymentservice.dto.PaymentRequest;
import com.example.paymentservice.dto.PaymentResponse;
import com.example.paymentservice.dto.messaging.PaymentReplyEvent;
import com.example.paymentservice.dto.messaging.ProcessPaymentCommand;
import com.example.paymentservice.dto.messaging.RefundPaymentCommand;
import com.example.paymentservice.service.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RabbitListener(queues = "payment.commands")
public class PaymentCommandListener {

    private static final Logger log = LoggerFactory.getLogger(PaymentCommandListener.class);
    
    private final PaymentService paymentService;
    private final RabbitTemplate rabbitTemplate;

    public PaymentCommandListener(PaymentService paymentService, RabbitTemplate rabbitTemplate) {
        this.paymentService = paymentService;
        this.rabbitTemplate = rabbitTemplate;
    }

    @RabbitHandler
    public void handleProcessPayment(ProcessPaymentCommand command) {
        log.info("Received ProcessPaymentCommand: {}", command);
        try {
            PaymentRequest request = PaymentRequest.builder()
                    .orderId(command.getOrderId())
                    .amount(command.getAmount())
                    .customerId(command.getCustomerId().toString())
                    .idempotencyKey(command.getIdempotencyKey())
                    .build();
            PaymentResponse response = paymentService.processPayment(request);
            
            PaymentReplyEvent reply = PaymentReplyEvent.builder()
                    .orderId(command.getOrderId())
                    .paymentId(response.getId())
                    .success(true)
                    .message("Payment processed successfully")
                    .build();
            rabbitTemplate.convertAndSend("saga.exchange", "order.reply.payment", reply);
        } catch (Exception e) {
            log.error("Failed to process payment for order: {}", command.getOrderId(), e);
            PaymentReplyEvent reply = PaymentReplyEvent.builder()
                    .orderId(command.getOrderId())
                    .success(false)
                    .message(e.getMessage())
                    .build();
            rabbitTemplate.convertAndSend("saga.exchange", "order.reply.payment", reply);
        }
    }

    @RabbitHandler
    public void handleRefundPayment(RefundPaymentCommand command) {
        log.info("Received RefundPaymentCommand: {}", command);
        try {
            paymentService.refundPayment(command.getPaymentId());
        } catch (Exception e) {
            log.error("Failed to refund payment: {}", command.getPaymentId(), e);
        }
    }
}
