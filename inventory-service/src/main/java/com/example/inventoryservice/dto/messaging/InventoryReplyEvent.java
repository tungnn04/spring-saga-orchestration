package com.example.inventoryservice.dto.messaging;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryReplyEvent {
    private String orderId;
    private UUID reservationId;
    private boolean success;
    private String message;
}
