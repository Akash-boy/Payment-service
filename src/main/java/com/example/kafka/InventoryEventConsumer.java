package com.example.kafka;

import com.example.dto.StockConfirmedEvent;

import com.example.dto.StockReservedEvent;
import com.example.service.PaymentService;
import com.fasterxml.jackson.databind.ObjectMapper; // ✅ correct import
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@AllArgsConstructor
@Slf4j
public class InventoryEventConsumer {

    private final PaymentService paymentService;
    private final ObjectMapper objectMapper;

    // ✅ ONE listener only — handles all event types from inventory-events topic
    @KafkaListener(
            topics = "${kafka.topic.inventory-events}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleInventoryEvent(
            @Payload Map<String, Object> message,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset
    ) {
        String eventType = (String) message.get("eventType");
        log.info("Received event: {} from topic: {}, partition: {}, offset: {}",
                eventType, topic, partition, offset);

        if (eventType == null) {
            log.warn("Received message with null eventType, skipping");
            return;
        }

        switch (eventType) {
            case "STOCK_RESERVED" -> {
                StockReservedEvent event = objectMapper.convertValue(message, StockReservedEvent.class);
                handleStockReserved(event);
            }
            case "STOCK_CONFIRMED" -> {                                        // ✅ add this
                StockConfirmedEvent event = objectMapper.convertValue(message, StockConfirmedEvent.class);
                handleStockConfirmed(event);
            }

            default -> log.debug("Ignoring unknown event type: {}", eventType);
        }
    }

    // ✅ private helper — not a listener, just business logic
    private void handleStockReserved(StockReservedEvent event) {
        log.info("Processing StockReserved for order: {}, reservationId: {}",
                event.getOrderId(), event.getReservationId());
        try {
            paymentService.initiatePaymentForOrder(event.getOrderId(), event.getReservationId());
            log.info("Payment initiated successfully for order: {}", event.getOrderId());
        } catch (Exception e) {
            log.error("Failed to initiate payment for order: {}, error: {}",
                    event.getOrderId(), e.getMessage(), e);
        }
    }
    // ✅ add this private method
    private void handleStockConfirmed(StockConfirmedEvent event) {
        log.info("Stock confirmed for order: {}, now completing payment", event.getOrderId());
        paymentService.completePayment(event.getOrderId(), event.getReservationId());
    }

}