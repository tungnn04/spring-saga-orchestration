package com.example.orderservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SagaResult {
    private boolean success;
    private String orderId;
    private String message;
    private String failedStep;
    private OrderResponse order;
}
