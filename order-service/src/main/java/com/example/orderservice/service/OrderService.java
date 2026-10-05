package com.example.orderservice.service;

import com.example.orderservice.client.InventoryClient;
import com.example.orderservice.client.PaymentClient;
import com.example.orderservice.domain.Order;
import com.example.orderservice.domain.OrderStatus;
import com.example.orderservice.dto.*;
import com.example.orderservice.exception.SagaExecutionException;
import com.example.orderservice.monitoring.SagaStepMetric;
import com.example.orderservice.repository.OrderRepository;
import com.example.orderservice.saga.SagaExecutor;
import com.example.orderservice.saga.SagaStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final PaymentClient paymentClient;
    private final InventoryClient inventoryClient;

    public OrderService(OrderRepository orderRepository,
                        PaymentClient paymentClient,
                        InventoryClient inventoryClient) {
        this.orderRepository = orderRepository;
        this.paymentClient = paymentClient;
        this.inventoryClient = inventoryClient;
    }

    @SagaStepMetric
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

        final Order savedOrder = order;
        log.info("[ORDER-SERVICE] Order created with id: {}", savedOrder.getId());

        // Build and execute the saga
        SagaExecutor executor = new SagaExecutor(savedOrder.getId().toString());

        // Step: Process Payment
        executor.addStep(new SagaStep<>(
                "process-payment",
                () -> {
                    updateOrderStatus(savedOrder.getId(), OrderStatus.PAYMENT_PROCESSING);
                    PaymentRequest paymentRequest = PaymentRequest.builder()
                            .orderId(savedOrder.getId())
                            .customerId(savedOrder.getCustomerId())
                            .amount(savedOrder.getTotalAmount())
                            .idempotencyKey("payment-" + savedOrder.getIdempotencyKey())
                            .build();
                    return paymentClient.processPayment(paymentRequest);
                },
                payment -> paymentClient.refundPayment(payment.getId()),
                3
        ));

        // Step: Reserve Inventory
        executor.addStep(new SagaStep<>(
                "reserve-inventory",
                () -> {
                    updateOrderStatus(savedOrder.getId(), OrderStatus.INVENTORY_RESERVING);
                    InventoryRequest inventoryRequest = InventoryRequest.builder()
                            .orderId(savedOrder.getId())
                            .productId(savedOrder.getProductId())
                            .quantity(savedOrder.getQuantity())
                            .idempotencyKey("inventory-" + savedOrder.getIdempotencyKey())
                            .build();
                    return inventoryClient.reserveStock(inventoryRequest);
                },
                reservation -> inventoryClient.releaseReservation(reservation.getId()),
                3
        ));

        SagaResult result = executor.run();

        if (result.isSuccess()) {
            updateOrderStatus(savedOrder.getId(), OrderStatus.COMPLETED);
            Order completed = orderRepository.findById(savedOrder.getId()).orElse(savedOrder);
            result.setOrderId(savedOrder.getId().toString());
            result.setOrder(mapToResponse(completed));
        } else {
            updateOrderStatus(savedOrder.getId(), OrderStatus.CANCELLED);
        }

        return result;
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
