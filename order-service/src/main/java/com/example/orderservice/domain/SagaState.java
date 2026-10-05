package com.example.orderservice.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "saga_states")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SagaState {
    @Id
    private String id;
    
    private UUID orderId;
    
    private String currentStep;
    
    private String status; // STARTED, COMPLETED, FAILED, COMPENSATING
    
    private String payload;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
}
