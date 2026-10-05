package com.example.inventoryservice.repository;

import com.example.inventoryservice.domain.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, UUID> {
    Optional<Reservation> findByIdempotencyKey(String idempotencyKey);
    Optional<Reservation> findByOrderId(UUID orderId);
}
