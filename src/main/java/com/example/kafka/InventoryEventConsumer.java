package com.example.kafka.consumer;

import com.example.dto.StockReservedEvent;
import com.example.service.PaymentService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
@Slf4j
public class InventoryEventConsumer {

    private final PaymentService paymentService;

    /**
     * Listen to StockReserved events from Inventory Service
     * Automatically initiate payment when stock is reserved
     */
    @KafkaListener(
            topics = "${kafka.topic.inventory-events:inventory-events}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void handleStockReserved(StockReservedEvent event) {

        if (!"STOCK_RESERVED".equals(event.getEventType())) {
            log.debug("Skipping event type: {}", event.getEventType());
            return;
        }

        log.info("Processing StockReserved event for order: {}, success: {}",
                event.getOrderId(), event.isSuccess());

        if (event.isSuccess()) {
            // Stock reserved successfully - initiate payment
            log.info("Stock reserved for order {}. Initiating payment...", event.getOrderId());

            try {
                // Initiate payment automatically
                paymentService.initiatePaymentForOrder(event.getOrderId(), event.getReservationId());

                log.info("Payment initiated successfully for order: {}", event.getOrderId());

            } catch (Exception e) {
                log.error("Failed to initiate payment for order {}: {}",
                        event.getOrderId(), e.getMessage(), e);
            }

        } else {
            // Stock reservation failed - notify order service
            log.warn("Stock reservation failed for order {}: {}",
                    event.getOrderId(), event.getMessage());

            // Could publish PaymentCancelled event here
        }
    }
}