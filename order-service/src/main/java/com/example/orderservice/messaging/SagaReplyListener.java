package com.example.orderservice.messaging;

import com.example.orderservice.domain.Order;
import com.example.orderservice.domain.OrderStatus;
import com.example.orderservice.domain.SagaState;
import com.example.orderservice.dto.messaging.InventoryReplyEvent;
import com.example.orderservice.dto.messaging.PaymentReplyEvent;
import com.example.orderservice.dto.messaging.RefundPaymentCommand;
import com.example.orderservice.dto.messaging.ReserveInventoryCommand;
import com.example.orderservice.repository.OrderRepository;
import com.example.orderservice.repository.SagaStateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RabbitListener(queues = "order.replies")
public class SagaReplyListener {

    private static final Logger log = LoggerFactory.getLogger(SagaReplyListener.class);

    private final OrderRepository orderRepository;
    private final SagaStateRepository sagaStateRepository;
    private final RabbitTemplate rabbitTemplate;

    public SagaReplyListener(OrderRepository orderRepository,
                             SagaStateRepository sagaStateRepository,
                             RabbitTemplate rabbitTemplate) {
        this.orderRepository = orderRepository;
        this.sagaStateRepository = sagaStateRepository;
        this.rabbitTemplate = rabbitTemplate;
    }

    @RabbitHandler
    public void handlePaymentReply(PaymentReplyEvent event) {
        log.info("Received PaymentReplyEvent for order {}: success={}", event.getOrderId(), event.isSuccess());
        SagaState sagaState = sagaStateRepository.findByOrderId(event.getOrderId())
                .orElseThrow(() -> new RuntimeException("SagaState not found"));
        Order order = orderRepository.findById(event.getOrderId())
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (event.isSuccess()) {
            sagaState.setCurrentStep("inventory");
            sagaState.setUpdatedAt(LocalDateTime.now());
            sagaStateRepository.save(sagaState);

            order.setStatus(OrderStatus.PAYMENT_PROCESSING); 
            orderRepository.save(order);

            ReserveInventoryCommand command = ReserveInventoryCommand.builder()
                    .orderId(order.getId())
                    .productId(order.getProductId())
                    .quantity(order.getQuantity())
                    .idempotencyKey("inv-" + order.getIdempotencyKey())
                    .build();
            rabbitTemplate.convertAndSend("saga.exchange", "inventory.command.reserve", command);
        } else {
            sagaState.setStatus("FAILED");
            sagaState.setUpdatedAt(LocalDateTime.now());
            sagaStateRepository.save(sagaState);

            order.setStatus(OrderStatus.CANCELLED);
            orderRepository.save(order);
        }
    }

    @RabbitHandler
    public void handleInventoryReply(InventoryReplyEvent event) {
        log.info("Received InventoryReplyEvent for order {}: success={}", event.getOrderId(), event.isSuccess());
        SagaState sagaState = sagaStateRepository.findByOrderId(event.getOrderId())
                .orElseThrow(() -> new RuntimeException("SagaState not found"));
        Order order = orderRepository.findById(event.getOrderId())
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (event.isSuccess()) {
            sagaState.setStatus("COMPLETED");
            sagaState.setUpdatedAt(LocalDateTime.now());
            sagaStateRepository.save(sagaState);

            order.setStatus(OrderStatus.COMPLETED);
            orderRepository.save(order);
        } else {
            sagaState.setStatus("COMPENSATING");
            sagaState.setUpdatedAt(LocalDateTime.now());
            sagaStateRepository.save(sagaState);

            order.setStatus(OrderStatus.CANCELLED);
            orderRepository.save(order);

            RefundPaymentCommand refundCommand = RefundPaymentCommand.builder()
                    .orderId(order.getId())
                    .paymentId("payment-" + order.getId().toString())
                    .build();
            rabbitTemplate.convertAndSend("saga.exchange", "payment.command.refund", refundCommand);
        }
    }
}
