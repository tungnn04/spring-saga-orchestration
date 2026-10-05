package com.example.orderservice.service;

import com.example.orderservice.domain.Order;
import com.example.orderservice.domain.OrderStatus;
import com.example.orderservice.domain.SagaState;
import com.example.orderservice.dto.OrderRequest;
import com.example.orderservice.dto.OrderResponse;
import com.example.orderservice.dto.SagaResult;
import com.example.orderservice.dto.messaging.ProcessPaymentCommand;
import com.example.orderservice.exception.SagaExecutionException;
import com.example.orderservice.repository.OrderRepository;
import com.example.orderservice.repository.SagaStateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final SagaStateRepository sagaStateRepository;
    private final RabbitTemplate rabbitTemplate;

    public OrderService(OrderRepository orderRepository,
                        SagaStateRepository sagaStateRepository,
                        RabbitTemplate rabbitTemplate) {
        this.orderRepository = orderRepository;
        this.sagaStateRepository = sagaStateRepository;
        this.rabbitTemplate = rabbitTemplate;
    }

    @Transactional
    public SagaResult createOrder(OrderRequest request) {
        log.info("[ORDER-SERVICE] Received order request with idempotencyKey: {}", request.getIdempotencyKey());

        // Idempotency check
        if (request.getIdempotencyKey() != null) {
            var existing = orderRepository.findByIdempotencyKey(request.getIdempotencyKey());
            if (existing.isPresent()) {
                log.info("[ORDER-SERVICE] Duplicate request detected, returning existing order");
                return SagaResult.builder()
                        .success(true)
                        .message("Order already processed (idempotent response)")
                        .orderId(existing.get().getId().toString())
                        .order(mapToResponse(existing.get()))
                        .build();
            }
        }

        // Step 1: Create order in PENDING state
        Order order = Order.builder()
                .customerId(request.getCustomerId())
                .productId(request.getProductId())
                .quantity(request.getQuantity())
                .totalAmount(request.getUnitPrice().multiply(new java.math.BigDecimal(request.getQuantity())))
                .status(OrderStatus.PENDING)
                .idempotencyKey(request.getIdempotencyKey() != null
                        ? request.getIdempotencyKey()
                        : UUID.randomUUID().toString())
                .build();
        order = orderRepository.save(order);

        Order savedOrder = order;
        log.info("[ORDER-SERVICE] Order created with id: {}", savedOrder.getId());

        // Save new SagaState in STARTED state
        SagaState sagaState = SagaState.builder()
                .id(UUID.randomUUID().toString())
                .orderId(savedOrder.getId())
                .currentStep("payment")
                .status("STARTED")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        sagaStateRepository.save(sagaState);

        // Send ProcessPaymentCommand to RabbitMQ
        ProcessPaymentCommand command = ProcessPaymentCommand.builder()
                .orderId(savedOrder.getId())
                .amount(savedOrder.getTotalAmount())
                .customerId(savedOrder.getCustomerId())
                .idempotencyKey("payment-" + savedOrder.getIdempotencyKey())
                .build();
        rabbitTemplate.convertAndSend("saga.exchange", "payment.command.process", command);

        return SagaResult.builder()
                .success(true)
                .message("Order creation initiated")
                .orderId(savedOrder.getId().toString())
                .order(mapToResponse(savedOrder))
                .build();
    }

    public OrderResponse getOrder(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new SagaExecutionException("Order not found: " + orderId));
        return mapToResponse(order);
    }

    public List<OrderResponse> getOrdersByCustomer(String customerId) {
        return orderRepository.findByCustomerId(customerId)
                .stream().map(this::mapToResponse).toList();
    }

    @Transactional
    public void updateOrderStatus(UUID orderId, OrderStatus status) {
        orderRepository.findById(orderId).ifPresent(o -> {
            o.setStatus(status);
            orderRepository.save(o);
        });
    }

    private OrderResponse mapToResponse(Order order) {
        return OrderResponse.builder()
                .id(order.getId())
                .customerId(order.getCustomerId())
                .productId(order.getProductId())
                .quantity(order.getQuantity())
                .totalAmount(order.getTotalAmount())
                .status(order.getStatus())
                .idempotencyKey(order.getIdempotencyKey())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }
}
