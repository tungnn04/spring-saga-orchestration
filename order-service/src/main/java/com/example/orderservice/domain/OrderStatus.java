package com.example.orderservice.domain;

public enum OrderStatus {
    PENDING,
    CONFIRMED,
    PAYMENT_PROCESSING,
    PAYMENT_FAILED,
    INVENTORY_RESERVING,
    INVENTORY_FAILED,
    COMPLETED,
    CANCELLED
}
