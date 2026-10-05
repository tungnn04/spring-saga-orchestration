package com.example.inventoryservice.dto.messaging;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReserveInventoryCommand {
    private String orderId;
    private String productId;
    private Integer quantity;
    private String idempotencyKey;
}
