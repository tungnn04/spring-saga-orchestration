package com.example.orderservice.repository;

import com.example.orderservice.domain.SagaState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SagaStateRepository extends JpaRepository<SagaState, String> {
    Optional<SagaState> findByOrderId(UUID orderId);
}
