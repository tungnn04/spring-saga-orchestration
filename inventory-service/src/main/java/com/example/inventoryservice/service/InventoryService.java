package com.example.inventoryservice.service;

import com.example.inventoryservice.domain.Product;
import com.example.inventoryservice.domain.Reservation;
import com.example.inventoryservice.domain.ReservationStatus;
import com.example.inventoryservice.dto.InventoryRequest;
import com.example.inventoryservice.dto.InventoryResponse;
import com.example.inventoryservice.repository.ProductRepository;
import com.example.inventoryservice.repository.ReservationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class InventoryService {

    private static final Logger log = LoggerFactory.getLogger(InventoryService.class);

    private final ProductRepository productRepository;
    private final ReservationRepository reservationRepository;

    public InventoryService(ProductRepository productRepository,
                            ReservationRepository reservationRepository) {
        this.productRepository = productRepository;
        this.reservationRepository = reservationRepository;
    }

    @Transactional
    public InventoryResponse reserveStock(InventoryRequest request) {
        log.info("[INVENTORY] Reserving stock for orderId: {}, productId: {}, qty: {}",
                request.getOrderId(), request.getProductId(), request.getQuantity());

        // Idempotency check
        if (request.getIdempotencyKey() != null) {
            var existing = reservationRepository.findByIdempotencyKey(request.getIdempotencyKey());
            if (existing.isPresent()) {
                log.info("[INVENTORY] Duplicate reservation request, returning existing");
                return mapToResponse(existing.get());
            }
        }

        // Pessimistic lock to prevent overselling
        Product product = productRepository.findByIdWithLock(request.getProductId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Product not found: " + request.getProductId()));

        if (product.getAvailableQuantity() < request.getQuantity()) {
            throw new IllegalStateException(
                    String.format("Insufficient stock for product %s: available=%d, requested=%d",
                            request.getProductId(), product.getAvailableQuantity(), request.getQuantity()));
        }

        // Reserve stock
        product.setAvailableQuantity(product.getAvailableQuantity() - request.getQuantity());
        product.setReservedQuantity(product.getReservedQuantity() + request.getQuantity());
        productRepository.save(product);

        Reservation reservation = Reservation.builder()
                .orderId(request.getOrderId())
                .productId(request.getProductId())
                .quantity(request.getQuantity())
                .status(ReservationStatus.ACTIVE)
                .idempotencyKey(request.getIdempotencyKey() != null
                        ? request.getIdempotencyKey()
                        : UUID.randomUUID().toString())
                .build();

        reservation = reservationRepository.save(reservation);
        log.info("[INVENTORY] Stock reserved successfully, reservationId: {}", reservation.getId());
        return mapToResponse(reservation);
    }

    @Transactional
    public InventoryResponse releaseReservation(UUID reservationId) {
        log.info("[INVENTORY] Releasing reservation: {}", reservationId);
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found: " + reservationId));

        if (reservation.getStatus() == ReservationStatus.RELEASED) {
            log.info("[INVENTORY] Reservation already released: {}", reservationId);
            return mapToResponse(reservation);
        }

        String productId = reservation.getProductId();
        Product product = productRepository.findByIdWithLock(productId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Product not found: " + productId));

        product.setAvailableQuantity(product.getAvailableQuantity() + reservation.getQuantity());
        product.setReservedQuantity(product.getReservedQuantity() - reservation.getQuantity());
        productRepository.save(product);

        reservation.setStatus(ReservationStatus.RELEASED);
        reservation = reservationRepository.save(reservation);
        log.info("[INVENTORY] Reservation released: {}", reservationId);
        return mapToResponse(reservation);
    }

    public InventoryResponse getReservation(UUID reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found: " + reservationId));
        return mapToResponse(reservation);
    }

    private InventoryResponse mapToResponse(Reservation reservation) {
        return InventoryResponse.builder()
                .id(reservation.getId())
                .orderId(reservation.getOrderId())
                .productId(reservation.getProductId())
                .quantity(reservation.getQuantity())
                .status(reservation.getStatus())
                .idempotencyKey(reservation.getIdempotencyKey())
                .createdAt(reservation.getCreatedAt())
                .updatedAt(reservation.getUpdatedAt())
                .build();
    }
}
