package com.example.inventoryservice.controller;

import com.example.inventoryservice.dto.InventoryRequest;
import com.example.inventoryservice.dto.InventoryResponse;
import com.example.inventoryservice.service.InventoryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @PostMapping("/reserve")
    public ResponseEntity<InventoryResponse> reserveStock(@RequestBody InventoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(inventoryService.reserveStock(request));
    }

    @DeleteMapping("/reservations/{reservationId}")
    public ResponseEntity<InventoryResponse> releaseReservation(@PathVariable UUID reservationId) {
        return ResponseEntity.ok(inventoryService.releaseReservation(reservationId));
    }

    @GetMapping("/reservations/{reservationId}")
    public ResponseEntity<InventoryResponse> getReservation(@PathVariable UUID reservationId) {
        return ResponseEntity.ok(inventoryService.getReservation(reservationId));
    }
}
