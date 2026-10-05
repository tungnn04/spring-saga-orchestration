package com.example.inventoryservice.messaging;

import com.example.inventoryservice.dto.InventoryRequest;
import com.example.inventoryservice.dto.InventoryResponse;
import com.example.inventoryservice.dto.messaging.InventoryReplyEvent;
import com.example.inventoryservice.dto.messaging.ReleaseInventoryCommand;
import com.example.inventoryservice.dto.messaging.ReserveInventoryCommand;
import com.example.inventoryservice.service.InventoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@RabbitListener(queues = "inventory.commands")
public class InventoryCommandListener {

    private static final Logger log = LoggerFactory.getLogger(InventoryCommandListener.class);
    private final InventoryService inventoryService;
    private final RabbitTemplate rabbitTemplate;

    public InventoryCommandListener(InventoryService inventoryService, RabbitTemplate rabbitTemplate) {
        this.inventoryService = inventoryService;
        this.rabbitTemplate = rabbitTemplate;
    }

    @RabbitHandler
    public void handleReserveInventory(@Payload ReserveInventoryCommand command) {
        log.info("Received ReserveInventoryCommand for order: {}", command.getOrderId());
        try {
            InventoryRequest request = new InventoryRequest();
            request.setOrderId(command.getOrderId());
            request.setProductId(command.getProductId());
            request.setQuantity(command.getQuantity());
            request.setIdempotencyKey(command.getIdempotencyKey());

            InventoryResponse response = inventoryService.reserveStock(request);

            InventoryReplyEvent replyEvent = InventoryReplyEvent.builder()
                    .orderId(command.getOrderId())
                    .reservationId(response.getId())
                    .success(true)
                    .message("Inventory reserved successfully")
                    .build();

            rabbitTemplate.convertAndSend("saga.exchange", "order.reply.inventory", replyEvent);
        } catch (Exception e) {
            log.error("Failed to reserve inventory for order: {}", command.getOrderId(), e);
            InventoryReplyEvent replyEvent = InventoryReplyEvent.builder()
                    .orderId(command.getOrderId())
                    .success(false)
                    .message(e.getMessage())
                    .build();
            rabbitTemplate.convertAndSend("saga.exchange", "order.reply.inventory", replyEvent);
        }
    }

    @RabbitHandler
    public void handleReleaseInventory(@Payload ReleaseInventoryCommand command) {
        log.info("Received ReleaseInventoryCommand for reservation: {}", command.getReservationId());
        try {
            inventoryService.releaseReservation(command.getReservationId());
        } catch (Exception e) {
            log.error("Failed to release reservation: {}", command.getReservationId(), e);
        }
    }
}
