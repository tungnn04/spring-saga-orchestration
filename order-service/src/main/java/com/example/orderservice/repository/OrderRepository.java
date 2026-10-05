package com.example.orderservice.repository;

import com.example.orderservice.domain.Order;
import com.example.orderservice.domain.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {
    Optional<Order> findByIdempotencyKey(String idempotencyKey);
    List<Order> findByCustomerId(String customerId);
    List<Order> findByStatus(OrderStatus status);
}
