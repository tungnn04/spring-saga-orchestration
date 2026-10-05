package com.example.orderservice.client;

import com.example.orderservice.dto.InventoryRequest;
import com.example.orderservice.dto.InventoryResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

@Component
public class InventoryClient {

    private static final Logger log = LoggerFactory.getLogger(InventoryClient.class);

    private final RestTemplate restTemplate;
    private final String inventoryServiceUrl;

    public InventoryClient(RestTemplate restTemplate,
                           @Value("${services.inventory.url}") String inventoryServiceUrl) {
        this.restTemplate = restTemplate;
        this.inventoryServiceUrl = inventoryServiceUrl;
    }

    public InventoryResponse reserveStock(InventoryRequest request) {
        log.info("[CLIENT] Reserving stock for order: {}", request.getOrderId());
        return restTemplate.postForObject(
                inventoryServiceUrl + "/api/inventory/reserve",
                request,
                InventoryResponse.class
        );
    }

    public void releaseReservation(UUID reservationId) {
        log.info("[CLIENT] Releasing reservation: {}", reservationId);
        restTemplate.delete(inventoryServiceUrl + "/api/inventory/reservations/" + reservationId);
    }
}
